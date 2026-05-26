const fs = require('fs');
const path = require('path');

const sourceDir = path.join(__dirname, '../../assets/images');
const targetDir = path.join(__dirname, '../public/images');

// Create target directory if it doesn't exist
if (!fs.existsSync(targetDir)) {
  fs.mkdirSync(targetDir, { recursive: true });
}

// Copy all files from assets/images to public/images
if (fs.existsSync(sourceDir)) {
  const files = fs.readdirSync(sourceDir);
  files.forEach((file) => {
    const src = path.join(sourceDir, file);
    const dest = path.join(targetDir, file);
    if (fs.statSync(src).isFile()) {
      fs.copyFileSync(src, dest);
    }
  });
  console.log('✓ Images copied from assets/images to public/images');
} else {
  console.log('ℹ No assets/images directory found (this is ok if you haven\'t added images yet)');
}
