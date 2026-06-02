const fs = require('fs');
const path = require('path');

/**
 * Recursively copy directory and all contents
 * @param {string} src - Source directory
 * @param {string} dest - Destination directory
 */
const copyDirRecursive = (src, dest) => {
  if (!fs.existsSync(dest)) {
    fs.mkdirSync(dest, { recursive: true });
  }

  const files = fs.readdirSync(src);
  files.forEach((file) => {
    const srcFile = path.join(src, file);
    const destFile = path.join(dest, file);
    const stat = fs.statSync(srcFile);

    if (stat.isDirectory()) {
      copyDirRecursive(srcFile, destFile);
    } else {
      fs.copyFileSync(srcFile, destFile);
    }
  });
};

// Source: assets/images (project root level)
const sourceDir = path.join(__dirname, '../../assets/images');
const targetDir = path.join(__dirname, '../public/images');

if (fs.existsSync(sourceDir)) {
  copyDirRecursive(sourceDir, targetDir);
  console.log('✓ Images copied from assets/images to public/images (recursive)');
} else {
  console.log('ℹ No assets/images directory found (this is ok if you haven\'t added images yet)');
}
