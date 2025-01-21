package com.github.acbgbca.pwscraper;

public enum FileType {
  PDF,
  PNG,
  JPEG,
  TEXT;

  static FileType getFileType(String filename) {
    String fileExtension =
      filename.indexOf('.') >= 0 ? filename.substring(filename.lastIndexOf('.') + 1) : "";
      fileExtension = fileExtension.toLowerCase();
    
    switch (fileExtension) {
      case "pdf":
        return PDF;
      case "png":
        return PNG;
      case "jpg":
      case "jpeg":
        return JPEG;
      default:
        return TEXT;
    }
  }
}
