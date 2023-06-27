package daff.searchengine.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesConfig {
    private List<SiteConfig> sites;
    private String agent;
    private String referer;
}
