package com.example.pbookmark.domain;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/*
Note that the entity class is not public,
so it’s visibility is limited to com.example.pbookmark.domain package.
 */
@Entity
@Table(name = "bookmarks")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(value = {AuditingEntityListener.class, AuditBookmark.class})
class Bookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String url;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    //@LastModifiedDate
    //@Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", insertable = false, updatable = true)
    private Instant updatedAt;

    /*@PreUpdate
    public void updateTimestamps(){
        this.updatedAt=Instant.now();
    }*/

    /*@PrePersist
    public void createTimestamps(){
        this.createdAt= Instant.now();
    }*/
}
