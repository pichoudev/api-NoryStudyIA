package com.norvya.norvya.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JsonRepairUtil {

    public String repair(String json) {
        if (json == null || json.isBlank()) return "{}";

        // Étape 1 : Nettoyer les backticks markdown
        json = json.trim();
        if (json.startsWith("```json")) json = json.substring(7);
        if (json.startsWith("```"))     json = json.substring(3);
        if (json.endsWith("```"))
            json = json.substring(0, json.length() - 3);
        json = json.trim();

        // Étape 2 : Supprimer les virgules avant ] ou }
        json = json.replaceAll(",\\s*]", "]");
        json = json.replaceAll(",\\s*}", "}");

        // Étape 3 : Corriger [, "val"] → ["val"]
        json = json.replaceAll("\\[\\s*,", "[");

        // Étape 4 : Corriger les doubles virgules
        json = json.replaceAll(",\\s*,", ",");

        // Étape 5 : Corriger "key": , → "key": null
        json = json.replaceAll("(\"[^\"]+\"\\s*:\\s*),", "$1null,");
        json = json.replaceAll("(\"[^\"]+\"\\s*:\\s*)}", "$1null}");

        // Étape 6 : Corriger les tableaux avec valeurs vides ["", ]
        json = json.replaceAll(",\\s*\"\"\\s*]", "]");

        // Étape 7 : Remplacer les valeurs null littérales manquantes
        json = json.replaceAll(":\\s*\n\\s*,", ": null,");
        json = json.replaceAll(":\\s*\n\\s*}", ": null}");

        // Étape 8 : Fermer les éléments non fermés
        json = closeOpenElements(json);

        log.debug("JSON réparé : {} caractères", json.length());
        return json;
    }

    private String closeOpenElements(String json) {
        int braces = 0, brackets = 0;
        boolean inString = false;
        char prev = 0;

        for (char c : json.toCharArray()) {
            if (c == '"' && prev != '\\') inString = !inString;
            if (!inString) {
                if (c == '{')      braces++;
                else if (c == '}') braces--;
                else if (c == '[') brackets++;
                else if (c == ']') brackets--;
            }
            prev = c;
        }

        StringBuilder sb = new StringBuilder(json);
        String trimmed = sb.toString().stripTrailing();
        if (trimmed.endsWith(",")) {
            int lastComma = sb.lastIndexOf(",");
            sb.deleteCharAt(lastComma);
        }

        for (int i = 0; i < brackets; i++) sb.append("]");
        for (int i = 0; i < braces; i++)   sb.append("}");

        return sb.toString();
    }
}