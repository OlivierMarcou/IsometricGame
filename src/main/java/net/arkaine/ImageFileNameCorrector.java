package net.arkaine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Classe pour corriger les noms des fichiers images dans les dossiers
 * sols, murs et plafonds pour les rendre compatibles avec le jeu
 */
public class ImageFileNameCorrector {

    private static final String RESOURCES_PATH = "src/main/resources/";
    private static final String[] DIRECTORIES = {"sol", "murs", "plafonds"};
    private static final String[] EXPECTED_PREFIXES = {"floor_", "wall_", "ceiling_"};
    private static final String[] EXTENSIONS = {".png", ".jpg", ".jpeg", ".PNG", ".JPG", ".JPEG"};

    public static void main(String[] args) {
        System.out.println("=== Correcteur de Noms de Fichiers Images ===");
        System.out.println();

        ImageFileNameCorrector corrector = new ImageFileNameCorrector();

        for (int i = 0; i < DIRECTORIES.length; i++) {
            corrector.correctDirectory(DIRECTORIES[i], EXPECTED_PREFIXES[i]);
        }

        System.out.println("=== Correction terminée ! ===");
    }

    public void correctDirectory(String directoryName, String expectedPrefix) {
        System.out.println("🔍 Traitement du dossier: " + directoryName);

        String fullPath = RESOURCES_PATH + directoryName;
        File directory = new File(fullPath);

        if (!directory.exists()) {
            System.out.println("❌ Dossier non trouvé: " + fullPath);
            System.out.println();
            return;
        }

        // Lister tous les fichiers images
        File[] files = directory.listFiles(this::isImageFile);
        if (files == null || files.length == 0) {
            System.out.println("ℹ️ Aucune image trouvée dans " + directoryName);
            System.out.println();
            return;
        }

        System.out.println("📁 " + files.length + " fichiers trouvés");

        // Séparer les fichiers déjà bien nommés des autres
        List<File> correctFiles = new ArrayList<>();
        List<File> filesToRename = new ArrayList<>();
        Set<Integer> usedNumbers = new HashSet<>();

        Pattern correctPattern = Pattern.compile("^" + Pattern.quote(expectedPrefix) + "\\d+\\.(png|jpg|jpeg)$",
                Pattern.CASE_INSENSITIVE);

        for (File file : files) {
            String fileName = file.getName();
            if (correctPattern.matcher(fileName).matches()) {
                correctFiles.add(file);
                // Extraire le numéro utilisé
                String numberPart = fileName.substring(expectedPrefix.length(),
                        fileName.lastIndexOf('.'));
                try {
                    int number = Integer.parseInt(numberPart);
                    usedNumbers.add(number);
                } catch (NumberFormatException e) {
                    // Fichier mal formé, à renommer
                    filesToRename.add(file);
                }
            } else {
                filesToRename.add(file);
            }
        }

        System.out.println("✅ " + correctFiles.size() + " fichiers déjà correctement nommés");
        System.out.println("🔄 " + filesToRename.size() + " fichiers à renommer");

        if (filesToRename.isEmpty()) {
            System.out.println("ℹ️ Rien à corriger dans " + directoryName);
            System.out.println();
            return;
        }

        // Détecter et supprimer les doublons
        Map<String, List<File>> duplicateGroups = findDuplicates(filesToRename);
        removeDuplicates(duplicateGroups);

        // Mettre à jour la liste après suppression des doublons
        filesToRename = new ArrayList<>();
        for (File file : files) {
            if (file.exists() && isImageFile(file) &&
                    !correctPattern.matcher(file.getName()).matches()) {
                filesToRename.add(file);
            }
        }

        // Trier les fichiers pour un renommage cohérent
        filesToRename.sort(Comparator.comparing(File::getName));

        // Renommer les fichiers
        int renamedCount = 0;
        for (File file : filesToRename) {
            int newNumber = getNextAvailableNumber(usedNumbers);
            String newFileName = expectedPrefix + newNumber + ".png";

            try {
                Path sourcePath = file.toPath();
                Path targetPath = Paths.get(directory.getAbsolutePath(), newFileName);

                // Copier et renommer (conversion automatique vers PNG)
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

                // Supprimer l'ancien fichier si différent
                if (!sourcePath.equals(targetPath)) {
                    Files.delete(sourcePath);
                }

                System.out.println("✅ " + file.getName() + " → " + newFileName);
                usedNumbers.add(newNumber);
                renamedCount++;

            } catch (Exception e) {
                System.err.println("❌ Erreur renommage " + file.getName() + ": " + e.getMessage());
            }
        }

        System.out.println("📊 Résumé pour " + directoryName + ":");
        System.out.println("  - Fichiers renommés: " + renamedCount);
        System.out.println("  - Total final: " + (correctFiles.size() + renamedCount) + " fichiers");
        System.out.println();
    }

    private boolean isImageFile(File file) {
        if (file.isDirectory()) return false;

        String fileName = file.getName().toLowerCase();
        for (String ext : EXTENSIONS) {
            if (fileName.endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, List<File>> findDuplicates(List<File> files) {
        Map<String, List<File>> sizeGroups = new HashMap<>();

        // Grouper par taille de fichier (méthode simple de détection de doublons)
        for (File file : files) {
            String sizeKey = String.valueOf(file.length());
            sizeGroups.computeIfAbsent(sizeKey, k -> new ArrayList<>()).add(file);
        }

        // Garder seulement les groupes avec plusieurs fichiers
        Map<String, List<File>> duplicates = new HashMap<>();
        for (Map.Entry<String, List<File>> entry : sizeGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.put(entry.getKey(), entry.getValue());
            }
        }

        return duplicates;
    }

    private void removeDuplicates(Map<String, List<File>> duplicateGroups) {
        int totalDuplicatesRemoved = 0;

        for (Map.Entry<String, List<File>> entry : duplicateGroups.entrySet()) {
            List<File> duplicates = entry.getValue();

            // Garder le premier fichier, supprimer les autres
            for (int i = 1; i < duplicates.size(); i++) {
                File duplicate = duplicates.get(i);
                try {
                    Files.delete(duplicate.toPath());
                    System.out.println("🗑️ Doublon supprimé: " + duplicate.getName());
                    totalDuplicatesRemoved++;
                } catch (Exception e) {
                    System.err.println("❌ Erreur suppression doublon " + duplicate.getName() + ": " + e.getMessage());
                }
            }
        }

        if (totalDuplicatesRemoved > 0) {
            System.out.println("🧹 " + totalDuplicatesRemoved + " doublons supprimés");
        }
    }

    private int getNextAvailableNumber(Set<Integer> usedNumbers) {
        int number = 0;
        while (usedNumbers.contains(number)) {
            number++;
        }
        return number;
    }

    /**
     * Méthode utilitaire pour nettoyer un dossier spécifique
     */
    public static void correctSpecificDirectory(String directoryName) {
        ImageFileNameCorrector corrector = new ImageFileNameCorrector();

        switch (directoryName.toLowerCase()) {
            case "sol":
                corrector.correctDirectory("sol", "floor_");
                break;
            case "murs":
                corrector.correctDirectory("murs", "wall_");
                break;
            case "plafonds":
                corrector.correctDirectory("plafonds", "ceiling_");
                break;
            default:
                System.err.println("❌ Dossier non reconnu: " + directoryName);
                System.out.println("Dossiers supportés: sol, murs, plafonds");
        }
    }

    /**
     * Méthode pour obtenir des statistiques sans modification
     */
    public static void analyzeDirectories() {
        System.out.println("=== Analyse des Dossiers Images (Sans Modification) ===");
        System.out.println();

        ImageFileNameCorrector corrector = new ImageFileNameCorrector();

        for (int i = 0; i < DIRECTORIES.length; i++) {
            String directoryName = DIRECTORIES[i];
            String fullPath = RESOURCES_PATH + directoryName;
            File directory = new File(fullPath);

            System.out.println("📁 Dossier: " + directoryName);

            if (!directory.exists()) {
                System.out.println("❌ Dossier non trouvé");
                System.out.println();
                continue;
            }

            File[] files = directory.listFiles(corrector::isImageFile);
            if (files == null || files.length == 0) {
                System.out.println("ℹ️ Aucune image trouvée");
                System.out.println();
                continue;
            }

            int correctCount = 0;
            int incorrectCount = 0;
            String expectedPrefix = EXPECTED_PREFIXES[i];
            Pattern correctPattern = Pattern.compile("^" + Pattern.quote(expectedPrefix) + "\\d+\\.(png|jpg|jpeg)$",
                    Pattern.CASE_INSENSITIVE);

            for (File file : files) {
                if (correctPattern.matcher(file.getName()).matches()) {
                    correctCount++;
                } else {
                    incorrectCount++;
                }
            }

            System.out.println("  Total: " + files.length + " fichiers");
            System.out.println("  ✅ Corrects: " + correctCount);
            System.out.println("  🔄 À corriger: " + incorrectCount);
            System.out.println();
        }
    }
}