# /ux-review — UX & Frontend Quality Review

Review the React component or page specified in $ARGUMENTS (or the most recently changed frontend file if no argument). If $ARGUMENTS is a domain name (wealth, health, profile, household), review all pages in that domain.

## Step 1 — Read context
- Read `documents/CONTEXT_PRIMER.md` for current project state
- Read `documents/FRONTEND_GUIDELINES.md` for the full standards

## Step 2 — Audit the file(s) against these criteria

### Layout & Styling
- [ ] Tailwind CSS only — no `style={{}}`, no CSS modules, no other frameworks
- [ ] Responsive: uses `sm:` / `md:` breakpoints where appropriate
- [ ] Consistent spacing using Tailwind scale (not arbitrary `p-[13px]`)
- [ ] Loading states shown with spinner or skeleton text
- [ ] Error states shown with red text or alert box
- [ ] Empty states have a friendly message (not a blank page)

### Accessibility (SonarQube S6819 + general a11y)
- [ ] No `<div role="button">` — use actual `<button type="button">`
- [ ] No `<div role="link">` — use `<a>` or `<Link>`
- [ ] All interactive elements have accessible labels (`aria-label` or visible text)
- [ ] `<input>` fields have associated `<label htmlFor>`
- [ ] Images have `alt` text
- [ ] Focus management works for modals/dialogs

### React Code Quality
- [ ] No business logic in render — API calls in `useEffect` or custom hooks only
- [ ] No `console.log` in committed code
- [ ] No inline `style={{}}` props
- [ ] Component under 200 lines — split if larger
- [ ] Props destructured in function signature
- [ ] Custom hook for any stateful logic reused across components
- [ ] `useCallback`/`useMemo` used where re-renders are expensive

### SonarQube JS Rules
- [ ] Optional chaining: `ref?.current?.click()` not `ref.current && ref.current.click()`
- [ ] `Blob#text()` not `FileReader#readAsText()` for async file reads
- [ ] `===` not `==` for equality
- [ ] All async functions have `try/catch`

### API Integration
- [ ] All API calls use `web/src/api/<domain>.js` — no raw `fetch()` in components
- [ ] `profile_id` passed as query param — never hardcoded
- [ ] Loading flag set before call, cleared in `finally`

## Step 3 — Report
For each violation, report:
```
FILE:LINE — Rule violated — What to change
```

Then: total issue count per category. If zero issues, say "UX review clean."

## Step 4 — Fix
Fix all issues found. Run after fixing:
```
cd web && npm run lint && npm run test:ci && npm run build
```
