package daff.searchengine.models;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLInsert;

import java.util.List;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lemma", uniqueConstraints={@UniqueConstraint(columnNames = {"lemma", "site_id"})})
@SQLInsert(sql = "insert into lemma (frequency, lemma, site_id) " +
        "values (?, ?, ?) ON DUPLICATE KEY UPDATE frequency = frequency + 1 ")


public class LemmaEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private SiteEntity site;

    @ManyToMany
    @JoinTable(name = "`index`",
            joinColumns = @JoinColumn(name = "lemma_id"),
            inverseJoinColumns = @JoinColumn(name = "page_id"))
    private List<PageEntity> pages;

}
