package daff.searchengine.models;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "site")
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "name", columnDefinition = "VARCHAR(255) NOT NULL")
    private String name;

    @Column(name = "url", columnDefinition = "VARCHAR(255) NOT NULL")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "enum('INDEXING', 'INDEXED', 'FAILED') NOT NULL")
    private StatusType status;

    @Column(name = "status_time", columnDefinition = "DATETIME NOT NULL")
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Transient
    private String agent;

    @Transient
    private String referer;
}
