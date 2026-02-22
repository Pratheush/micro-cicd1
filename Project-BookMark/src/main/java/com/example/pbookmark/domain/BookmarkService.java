package com.example.pbookmark.domain;

import com.example.pbookmark.domain.xception.BookmarkNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *The BookmarkService is a transactional service layer component which will be used by
 * web layer or other service layer components. The BookmarkService class is annotated with
 * @Transactional(readOnly = true) which means all the public methods are transactional
 * and allows only read-only operations on the database.
 * We can override this read-only behaviour for the methods which needs to perform
 * insert/update/delete database operations by adding @Transactional annotation.
 *
 * @Cacheable caches paginated results by page number and size.
 * @CacheEvict(allEntries = true) clears cache whenever data changes (create/update/delete).
 * This ensures cache consistency.
 *
 */
@Service
@Transactional(readOnly = true)
public class BookmarkService {
    private final BookmarkRepository repo;

    BookmarkService(BookmarkRepository repo) {
        this.repo = repo;
    }

    /*
    The findAll() method will load all the records in the table and this may lead to OutOfMemoryExceptions
    if there are millions of records. If the table is ever-growing with new data, it is always advised
    to use Pagination.
     */
    /*public List<Bookmark> findAll() {
        return repo.findAll();
    }*/


    @Cacheable(value = "bookmarks", key = "#query.pageNo() + '-' + #query.pageSize()")
    public PagedResult<BookmarkDTO> findBookmarks(FindBookmarksQuery query) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        //from user POV, page number starts from 1, but for Spring Data JPA page number starts from 0.
        int pageNo = query.pageNo() > 0 ? query.pageNo() - 1 : 0;
        Pageable pageable = PageRequest.of(pageNo, query.pageSize(), sort);
        Page<BookmarkDTO> page = repo.findBookmarks(pageable);
        return new PagedResult<BookmarkDTO>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1, // for user page number starts from 1
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }


    public BookmarkDTO create(BookmarkDTO bookmark) {
        return new BookmarkDTO(0L,null,null,null);
    }

    @CacheEvict(value = "bookmarks", allEntries = true)
    @Transactional
    public BookmarkDTO create(CreateBookmarkCommand cmd) {
        Bookmark bookmark = new Bookmark();
        bookmark.setTitle(cmd.title());
        bookmark.setUrl(cmd.url());
       // bookmark.setCreatedAt(Instant.now());
        return BookmarkDTO.from(repo.save(bookmark));
    }

    @CacheEvict(value = "bookmarks", allEntries = true)
    @Transactional
    public void update(UpdateBookmarkCommand cmd) {
        Bookmark bookmark = repo.findById(cmd.id())
                .orElseThrow(() -> BookmarkNotFoundException.of(cmd.id()));
        bookmark.setTitle(cmd.title());
        bookmark.setUrl(cmd.url());
        //bookmark.setUpdatedAt(Instant.now());
        repo.save(bookmark);
    }

   /* public Optional<BookmarkDTO> findById(Long id) {
        return repo.findBookmarkById(id);
    }*/

    @Cacheable(value = "bookmarks", key = "#id")
    public BookmarkDTO findById(Long id) {
        //return repo.findBookmarkById(id);
        return repo.findBookmarkById(id)
                .orElseThrow(() -> BookmarkNotFoundException.of(id));
    }

    @CacheEvict(value = "bookmarks", allEntries = true)
    @Transactional
    public void delete(Long postId) {
        Bookmark entity = repo.findById(postId)
                .orElseThrow(()-> BookmarkNotFoundException.of(postId));
        repo.delete(entity);
    }
}