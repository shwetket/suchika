# Assets Directory

Store all static assets (images, icons, etc.) here.

## Structure

```
assets/
├── images/          ← All PNG, JPG, SVG, etc. go here
└── README.md        ← This file
```

## Usage in React

Images are automatically copied to `web/public/images/` at build/start time.

Reference them in JSX:

```jsx
// Direct path (public folder)
<img src="/images/my-image.png" alt="Description" />

// Or import (for bundling)
import myImage from '../../assets/images/my-image.png';
<img src={myImage} alt="Description" />
```

## Adding Images

1. Add image file to `assets/images/`
2. Run `npm start` or `npm run build` in `/web` (copies automatically)
3. Reference in React code as shown above

## Notes

- AI agents can read this folder without image loading issues
- Build process copies images to `web/public/images/` automatically
- Keep images organized by domain or feature if needed
