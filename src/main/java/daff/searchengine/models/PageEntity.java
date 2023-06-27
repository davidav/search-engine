package daff.searchengine.models;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "page")
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Exclude
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "path", columnDefinition = "TEXT NOT NULL, UNIQUE KEY pathIndex (site_id, path(512))")
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private SiteEntity site;

    @ManyToMany(mappedBy = "pages", cascade = CascadeType.REMOVE)
    private List<LemmaEntity> lemmas;

}
