package com.example.pbookmark.api.controllers;

import com.example.pbookmark.api.controllers.models.CreateBookmarkRequest;
import com.example.pbookmark.api.controllers.models.UpdateBookmarkRequest;
import com.example.pbookmark.domain.*;
import com.example.pbookmark.domain.errorresponse.ApiError;
import com.example.pbookmark.domain.xception.BookmarkTitleNotAllowedException;
import com.example.pbookmark.domain.xception.DuplicateBookmarkException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/bookmarks")
@Slf4j
class BookmarkController {
    private final BookmarkService bookmarkService;

    BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    /*
    exposing database entity as REST API response directly is a bad practice
    If we have to make any changes to the entity then API response format will be changed too which might not be desirable.
    So, we should create a DTO and expose only the necessary fields for that API.

    If we are fetching the data only to return to the client,
    then it is better to use DTO projections instead of loading entities.
     */
    /*@GetMapping
    List<Bookmark> findAll() {
        return bookmarkService.findAll();
    }*/

    @GetMapping
    PagedResult<BookmarkDTO> findBookmarks(
            @RequestParam(name = "page", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "size", defaultValue = "10") Integer pageSize) {
        log.info("BookmarkController API Request: Find Bookmarks page={}, size={}", pageNo, pageSize);
        FindBookmarksQuery query = new FindBookmarksQuery(pageNo, pageSize);
        return bookmarkService.findBookmarks(query);
    }

    /*

     */
    /*@PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookmarkDTO create(@RequestBody @Validated BookmarkDTO bookmark) {
        return bookmarkService.create(bookmark);
    }*/

    @PostMapping
    ResponseEntity<BookmarkDTO> create(@RequestBody @Validated CreateBookmarkRequest request) {

        String title=request.title();
        if(title.contains("fuck")) throw new BookmarkTitleNotAllowedException("This Title is Not Allowed");

        CreateBookmarkCommand cmd = new CreateBookmarkCommand(request.title(), request.url());
        BookmarkDTO bookmark = bookmarkService.create(cmd);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/api/bookmarks/{id}")
                .buildAndExpand(bookmark.id())
                .toUri();
        return ResponseEntity.created(location).body(bookmark);
        //return new ResponseEntity<>(bookmark,HttpStatus.CREATED);
    }


    /*@PostMapping
    ResponseEntity<BookmarkDTO> create(@RequestBody @Validated CreateBookmarkRequest request) {
        CreateBookmarkCommand cmd = new CreateBookmarkCommand(
                request.title(),
                request.url()
        );
        try {
            BookmarkDTO bookmark = bookmarkService.create(cmd);
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/api/bookmarks/{id}")
                    .buildAndExpand(bookmark.id()).toUri();
            return ResponseEntity.created(location).body(bookmark);
        } catch(DuplicateBookmarkException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch(BookmarkTitleNotAllowedException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }*/



    @PutMapping("/{id}")
    void update(@PathVariable(name = "id") Long id,
                @RequestBody @Validated UpdateBookmarkRequest request) {
        UpdateBookmarkCommand cmd = new UpdateBookmarkCommand(id, request.title(), request.url());
        bookmarkService.update(cmd);
    }

    /*@GetMapping("/{id}")
    ResponseEntity<BookmarkDTO> findById(@PathVariable(name = "id") Long id) {
        return bookmarkService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }*/

    @GetMapping("/{id}")
    ResponseEntity<BookmarkDTO> findById(@PathVariable(name = "id") Long id) {
        log.info("BookmarkController API Request: Find Bookmark id={}", id);
        BookmarkDTO bookmarkDTO=bookmarkService.findById(id);
        log.info("BookmarkController API Response: Bookmark DTO={}", bookmarkDTO);
        return ResponseEntity.ofNullable(bookmarkDTO);
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable(name = "id") Long id) {
        bookmarkService.delete(id);
    }


    @ExceptionHandler(DuplicateBookmarkException.class)
    public ResponseEntity<ApiError> handleDuplicateBookmarkException(DuplicateBookmarkException e) {
        ApiError error = new ApiError(e.getMessage());
        error.setStatus(HttpStatus.BAD_REQUEST);
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /*@ExceptionHandler({BookmarkTitleNotAllowedException.class})
    public ResponseEntity<ApiError> handleBookmarkTitleNotAllowedException(BookmarkTitleNotAllowedException e) {
        ApiError error = new ApiError(e.getMessage());
        error.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        error.setTimestamp(LocalDateTime.now());
        //error.setErrors(Arrays.asList(e.getMessage()));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }*/


}