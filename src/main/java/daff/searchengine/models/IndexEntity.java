package daff.searchengine.models;



import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "`index`")
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "`rank`", nullable = false)
    private float rank;

    @Column(name = "lemma_id")
    private int lemmaId;


    @Column(name = "page_id")
    private int pageId;

}
