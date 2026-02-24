package com.example.demo.service;

import com.example.demo.model.BirthdayModel;
import com.example.demo.repository.BirthdayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BirthdayService {

    //Папка uploads/birthdays 
    private static final Path UPLOAD_DIR = Paths.get("uploads", "birthdays").toAbsolutePath().normalize();
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    @Autowired
    private BirthdayRepository birthdayRepository;

    //Получить весь список 
    public List<BirthdayModel> getAllBirthdays() {

        return birthdayRepository.findAll();

    }

    //Сегодняшние
    public List<BirthdayModel> getTodayBirthdays() {

        LocalDate now = LocalDate.now();
        MonthDay today = MonthDay.from(now);


        //Получаем все записи из БД и через Stream API
        //Отбрасываем записи без даты рождения (защита от null)
        //Приводим дату рождения к формату MonthDay (день + месяц), с учётом обработки 29 февраля в невисокосный год
        //Оставляем только тех, у кого день и месяц совпадают с сегодняшним
        //Возвращаем результат в виде списка
        return birthdayRepository.findAll().stream()
                .filter(b -> b.getBirthdayDate() != null)
                .filter(b -> effectiveMonthDay(b.getBirthdayDate(), now.getYear()).equals(today))
                .toList();
    }

    //Ближайшие н дней
    public List<BirthdayModel> getUpcomingBirthdays(int days) {
        if (days <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be > 0");
        }

        LocalDate now = LocalDate.now();
        LocalDate end = now.plusDays(days);

        //Исключаем без даты
        //Следующую дату ДР (с учётом перехода и 29 февраля)
        //Оставляем ДР в диапазоне now, end
        //Сортируем по ближайшей дате наступления
        return birthdayRepository.findAll().stream()
                .filter(b -> b.getBirthdayDate() != null)
                .map(b -> new BirthdayWithNextDate(b, nextOccurrence(now, b.getBirthdayDate())))
                .filter(x -> !x.nextDate.isBefore(now) && !x.nextDate.isAfter(end))
                .sorted(Comparator.comparing(x -> x.nextDate))
                .map(x -> x.model)
                .toList();
    }

    //Добавление
    public BirthdayModel addBirthday(BirthdayModel birthday) {
        //Зануляем id кдиента чтобы он не накосячил
        birthday.setId(null);
        return birthdayRepository.save(birthday);
    }

    //Обновление
    public BirthdayModel updateBirthday(int id, BirthdayModel birthday) {
        BirthdayModel existing = birthdayRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Birthday not found: " + id));

        //Удаление фото
        existing.setName(birthday.getName());
        existing.setBirthdayDate(birthday.getBirthdayDate());
        return birthdayRepository.save(existing);
    }

    //Удаление
    public void deleteBirthday(int id) {
        BirthdayModel existing = birthdayRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Birthday not found: " + id));

        //Удаляем фото, если было
        if (existing.getPhotoFilename() != null) {
            deletePhotoFileIfExists(existing.getPhotoFilename());
        }

        birthdayRepository.deleteById(id);
    }

    
    //ФОТО загрузить\обновить\удалить

    public BirthdayModel uploadPhoto(int id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }

        BirthdayModel existing = birthdayRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Birthday not found: " + id));

        String ext = getSafeExtension(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXT.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type. Allowed: jpg, jpeg, png, webp");
        }

        ensureUploadDir();

        //Удаляем старое фото
        if (existing.getPhotoFilename() != null) {
            deletePhotoFileIfExists(existing.getPhotoFilename());
        }

        String newFilename = id + "_" + UUID.randomUUID() + "." + ext;
        Path target = UPLOAD_DIR.resolve(newFilename).normalize();

        // Защита от выходов из директории
        if (!target.startsWith(UPLOAD_DIR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save photo");
        }

        existing.setPhotoFilename(newFilename);
        return birthdayRepository.save(existing);
    }

    public Resource loadPhotoAsResource(int id) {
        BirthdayModel existing = birthdayRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Birthday not found: " + id));

        String filename = existing.getPhotoFilename();
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not set for id: " + id);
        }

        Path path = UPLOAD_DIR.resolve(filename).normalize();
        if (!path.startsWith(UPLOAD_DIR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid photo path");
        }

        try {
            Resource res = new UrlResource(path.toUri());
            if (!res.exists() || !res.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file missing");
            }
            return res;
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read photo");
        }
    }

    public void deletePhoto(int id) {
        BirthdayModel existing = birthdayRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Birthday not found: " + id));

        String filename = existing.getPhotoFilename();
        if (filename != null) {
            deletePhotoFileIfExists(filename);
        }
        existing.setPhotoFilename(null);
        birthdayRepository.save(existing);
    }

    private void ensureUploadDir() {
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create upload dir");
        }
    }

    private void deletePhotoFileIfExists(String filename) {
        try {
            Path p = UPLOAD_DIR.resolve(filename).normalize();
            if (p.startsWith(UPLOAD_DIR)) {
                Files.deleteIfExists(p);
            }
        } catch (IOException ignored) {
            //Удаление файла не должно ломать удаление записи
        }
    }

    private String getSafeExtension(String originalName) {
        
        if (originalName == null) return null;
        String name = originalName.trim().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return null;
        String ext = name.substring(dot + 1);
        //Минимальная защита от странных имен
        if (ext.length() > 5) return null;
        return ext;
    }

    
    //29 февраля и високосный год

    private MonthDay effectiveMonthDay(LocalDate storedDate, int targetYear) {

        MonthDay md = MonthDay.from(storedDate);
        if (md.equals(MonthDay.of(2, 29)) && !Year.isLeap(targetYear)) {
            return MonthDay.of(2, 28);
        }
        return md;
    }

    private LocalDate occurrenceInYear(LocalDate storedDate, int year) {

        MonthDay md = effectiveMonthDay(storedDate, year);
        return md.atYear(year);
    }

    private LocalDate nextOccurrence(LocalDate now, LocalDate storedDate) {

        LocalDate candidate = occurrenceInYear(storedDate, now.getYear());
        if (candidate.isBefore(now)) {
            candidate = occurrenceInYear(storedDate, now.getYear() + 1);
        }
        return candidate;
    }

    private static class BirthdayWithNextDate {

        final BirthdayModel model;
        final LocalDate nextDate;

        BirthdayWithNextDate(BirthdayModel model, LocalDate nextDate) {
            this.model = model;
            this.nextDate = nextDate;
        }
    }
}
