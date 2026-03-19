package com.example.pbookmark.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/*
Bookmark entity and BookmarkRepository are not public. They are package-private scoped classes/interfaces.
They are supposed to be used by BookmarkService only and is hidden from outside the
com.example.pbookmark.domain package.
 */
@Repository
interface BookmarkRepository extends JpaRepository<Bookmark,Long> {

    // Return paginated DTOs for bookmarks
    @Query("""
               SELECT
                new com.example.pbookmark.domain.BookmarkDTO(b.id, b.title, b.url, b.createdAt, b.createdBy, b.updatedBy)
               FROM Bookmark b
            """)
    Page<BookmarkDTO> findBookmarks(Pageable pageable);

    // Return a single bookmark DTO by ID
    @Query("""
           SELECT
            new com.example.pbookmark.domain.BookmarkDTO(b.id, b.title, b.url, b.createdAt, b.createdBy, b.updatedBy)
           FROM Bookmark b
           WHERE b.id = :Id
        """)
    Optional<BookmarkDTO> findBookmarkById(@Param("Id") Long id);
}
