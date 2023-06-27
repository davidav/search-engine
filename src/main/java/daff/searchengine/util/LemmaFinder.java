package daff.searchengine.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class LemmaFinder {
    private final LuceneMorphology luceneMorphology;
    private final String[] PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};

    @Autowired
    public LemmaFinder(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    public Map<String, Float> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Float> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank() || word.length() < 2) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            for (String normalWord : normalForms) {
                if (lemmas.containsKey(normalWord)) {
                    lemmas.put(normalWord, lemmas.get(normalWord) + 1);
                } else {
                    lemmas.put(normalWord, 1F);
                }
            }
        }
        return lemmas;
    }

    public Set<String> getLemmaSet(String text) {
        String[] textArray = arrayContainsRussianWords(text);
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {
            if (!word.isEmpty() && isCorrectWordForm(word)) {
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }
                lemmaSet.addAll(luceneMorphology.getNormalForms(word));
            }
        }
        return lemmaSet;
    }

    public synchronized List<String> getNormalForms(@NotNull String word){
        List<String> words = new ArrayList<>();
        if (word.isBlank() && isCorrectWordForm(word)) {
            return words;
        }
        List<String> wordBaseForms = getMorphInfo(word);
        if (anyWordBaseBelongToParticle(wordBaseForms)) {
            return words;
        }
        return luceneMorphology.getNormalForms(word);
    }

    public synchronized String[] arrayContainsRussianWords(@NotNull String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private List<String> getMorphInfo(String word){
        return luceneMorphology.getMorphInfo(word);
    }
    private boolean anyWordBaseBelongToParticle(@NotNull List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }
    private boolean hasParticleProperty(String wordBase) {
        for (String property : PARTICLES_NAMES) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }
    private boolean isCorrectWordForm(String word) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
            if (morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }
}
