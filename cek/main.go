package main

import (
	"encoding/xml"
	"os"
	"path/filepath"
	"strings"

	"github.com/rs/zerolog"
)

// Fungsi untuk mendaki ke atas mencari root project jika tool dijalankan dari sub-direktori
func findProjectRoot() (string, error) {
	dir, err := os.Getwd()
	if err != nil {
		return "", err
	}

	for {
		// Cek penanda root project Android/Telegram (bisa settings.gradle atau settings.gradle.kts)
		if fileExists(filepath.Join(dir, "settings.gradle")) || fileExists(filepath.Join(dir, "settings.gradle.kts")) {
			return dir, nil
		}

		parent := filepath.Dir(dir)
		if parent == dir {
			break
		}
		dir = parent
	}
	return os.Getwd() // Fallback ke direktori aktif jika tidak ketemu
}

func fileExists(filename string) bool {
	info, err := os.Stat(filename)
	if os.IsNotExist(err) {
		return false
	}
	return !info.IsDir()
}

func main() {
	logger := zerolog.New(os.Stdout).With().Timestamp().Logger()

	rootPath, err := findProjectRoot()
	if err != nil {
		logger.Fatal().Err(err).Msg("Gagal mendeteksi root project")
	}

	logger.Info().Str("path", rootPath).Msg("Project root terdeteksi. Memulai pemindaian...")

	// Validasi fleksibel untuk settings.gradle / settings.gradle.kts
	hasSettings := fileExists(filepath.Join(rootPath, "settings.gradle")) || fileExists(filepath.Join(rootPath, "settings.gradle.kts"))
	if !hasSettings {
		logger.Error().Msg("File esensial hilang: settings.gradle atau settings.gradle.kts tidak ditemukan di root!")
		os.Exit(1)
	}

	// Validasi AndroidManifest utama
	manifestPath := filepath.Join(rootPath, "app/src/main/AndroidManifest.xml")
	if !fileExists(manifestPath) {
		logger.Error().Str("file", manifestPath).Msg("AndroidManifest.xml utama tidak ditemukan!")
		os.Exit(1)
	}

	// Pemindaian XML di res/
	resDir := filepath.Join(rootPath, "app/src/main/res")
	var xmlErrorsCount int

	if fileExists(resDir) {
		err = filepath.Walk(resDir, func(path string, info os.FileInfo, err error) error {
			if err != nil {
				return err
			}
			if !info.IsDir() && strings.HasSuffix(info.Name(), ".xml") {
				content, readErr := os.ReadFile(path)
				if readErr != nil {
					return readErr
				}
				var v interface{}
				if parseErr := xml.Unmarshal(content, &v); parseErr != nil {
					logger.Error().
						Str("file", strings.TrimPrefix(path, rootPath+string(os.PathSeparator))).
						Err(parseErr).
						Msg("XML Syntax Error!")
					xmlErrorsCount++
				}
			}
			return nil
		})
	}

	if xmlErrorsCount > 0 {
		logger.Error().Int("total_errors", xmlErrorsCount).Msg("Validasi gagal karena ditemukan error XML.")
		os.Exit(1)
	}

	logger.Info().Msg("Struktur project bersih dan valid!")
}
