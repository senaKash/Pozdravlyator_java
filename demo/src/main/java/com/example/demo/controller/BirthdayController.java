package com.example.demo.controller;

import com.example.demo.model.BirthdayModel;
import com.example.demo.service.BirthdayService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/birthdays")
public class BirthdayController {

    @Autowired
    private BirthdayService birthdayService;

    @GetMapping
    @Operation(summary = "Получить весь список дней рождений")
    public List<BirthdayModel> getAllBirthdays() {
        
        return birthdayService.getAllBirthdays();
    }

    @GetMapping("/today")
    @Operation(summary = "Получить список сегодняшних именинников (с учётом 29 февраля)")
    public List<BirthdayModel> getTodayBirthdays() {

        return birthdayService.getTodayBirthdays();
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Получить список ближайших дней рождений (по умолчанию 7 дней, с учётом 29 февраля и високосного года)")
    public List<BirthdayModel> getUpcomingBirthdays(
            @RequestParam(name = "days", required = false, defaultValue = "7") int days
    ) 
    {
        return birthdayService.getUpcomingBirthdays(days);
    }

    @PostMapping
    @Operation(summary = "Добавить запись о дне рождения")
    public BirthdayModel addBirthday(@RequestBody BirthdayModel birthday) {

        return birthdayService.addBirthday(birthday);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Редактировать запись о дне рождения (не трогает фото)")
    public BirthdayModel updateBirthday(@PathVariable int id, @RequestBody BirthdayModel birthday) {

        return birthdayService.updateBirthday(id, birthday);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись о дне рождения (и фото, если есть)")
    public void deleteBirthday(@PathVariable int id) {

        birthdayService.deleteBirthday(id);
    }

    
    //ФОТО загрузить\обновить\удалить

    @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить/заменить фото именинника (multipart поле 'file')")
    public BirthdayModel uploadPhoto(@PathVariable int id, @RequestPart("file") MultipartFile file) {

        return birthdayService.uploadPhoto(id, file);
    }

    @GetMapping("/{id}/photo")
    @Operation(summary = "Получить фото именинника")
    public ResponseEntity<Resource> getPhoto(@PathVariable int id) {

        Resource res = birthdayService.loadPhotoAsResource(id);

        String filename = res.getFilename();
        MediaType mediaType = MediaType.IMAGE_JPEG; // по умолчанию

        if (filename != null) {
            if (filename.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (filename.endsWith(".webp")) {
                mediaType = MediaType.valueOf("image/webp");
            } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            }
        }

        return ResponseEntity.ok().contentType(mediaType).body(res);
    }

    @DeleteMapping("/{id}/photo")
    @Operation(summary = "Удалить фото именинника")
    public void deletePhoto(@PathVariable int id) {
        birthdayService.deletePhoto(id);
    }
}




















/*

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись о дне рождения")
    public void deleteBirthday(@PathVariable int id) {
        birthdayService.deleteBirthday(id);
    }
}

*/