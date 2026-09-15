# Test Plan

Working checklist for test coverage on JavaJambs. Update as features change —
add new cases, strike out ones that no longer apply, check off what's written.

Goal: 90%+ coverage, tests fail when logic breaks (not just when it's touched),
every feature covered from the user's perspective as well as unit-level.

## Layers

- [ ] **Unit tests** (JUnit 5 + Mockito) — service/business logic in isolation
- [ ] **Repository tests** (`@DataJpaTest`) — against real Flyway-migrated schema
- [ ] **Web-layer tests** (`@WebMvcTest` + MockMvc) — controllers, status codes, validation
- [ ] **Security tests** — route protection, auth/unauth behavior
- [ ] **Acceptance tests** (`@SpringBootTest`) — full user journeys end to end

## Edge cases

### `RegisterRequest` validation
(`src/main/java/com/javajambs/cher/auth/RegisterRequest.java`)

- [ ] Username: 2 chars (fail), 3 chars (pass), 50 chars (pass), 51 chars (fail)
- [ ] Username: blank / whitespace-only (fail)
- [ ] Password: 7 chars (fail), 8 chars (pass), 100 chars (pass), 101 chars (fail)
- [ ] Email: missing `@`, missing domain, blank, malformed-but-plausible (`user@`, `@domain.com`)
- [ ] Null vs empty string for each field (form binding may treat these differently)

### Username uniqueness — `UserService.registerUser`
(`src/main/java/com/javajambs/cher/user/UserService.java`)

- [ ] Exact duplicate username → `UsernameAlreadyExistsException`
- [ ] Case-variant duplicate (`"Jen"` vs `"jen"`) — confirm actual (likely case-sensitive) behavior is intended
- [ ] Leading/trailing whitespace duplicate (`"jen "` vs `"jen"`) — likely NOT caught, confirm if that's acceptable
- [ ] Failed registration attempt doesn't leave a partial/orphaned row behind

### `User` entity constraints
(`src/main/java/com/javajambs/cher/user/User.java`)

- [ ] Email has no uniqueness constraint — confirm two users CAN currently register with the same email; flag if unintended
- [ ] Registration/creation works with `bio` and `profile_picture` absent (nullable fields)
- [ ] `role` defaults to `"USER"`, `getAuthorities()` reflects it without being set explicitly

### `loadUserByUsername`
(`src/main/java/com/javajambs/cher/user/UserService.java`)

- [ ] Nonexistent username → `UsernameNotFoundException` (used by Spring Security login — must fail cleanly, not 500)
- [ ] Empty-string / null username passed through

### Session / equality
(`src/main/java/com/javajambs/cher/user/User.java`)

- [ ] `equals()` is id-based — two unsaved `User` instances (`id == null`) are never equal, even to a copy of themselves; confirm this doesn't break session/login handling

## Accessibility

No accessibility tests exist yet, and there's minimal UI to test against so far
(only `main.html` — the `register` template referenced by `AuthController` doesn't
exist yet). Findings from a manual read of `main.html`:

- [ ] Nav items ("Home", "About us", "Contact", "Login") are `<p>` tags, not links
      or a `<nav>` landmark — not keyboard-focusable, not announced as navigation
- [ ] `<title>Document</title>` is a placeholder, not descriptive
- [ ] No skip-to-content link
- [ ] `#main` is a `<div>`, not a `<main>` landmark element
- [ ] No visible focus styles confirmed yet (Tailwind CSS not built out enough to tell)
- [ ] Once forms exist (register/login): confirm labels are properly associated with inputs

Recommended tooling once there's more UI: automated `axe-core` checks (e.g. via
Playwright + `@axe-core/playwright`) wired into CI, plus a manual keyboard-only
navigation pass on key pages.

## Not yet covered / open questions

- [ ] Is there (or should there be) a global exception handler (`@ControllerAdvice`) for consistent error responses across controllers?
- [ ] Security route-protection matrix (which paths are public vs. authenticated) — needs enumerating once more routes exist
- [ ] CI test-DB strategy (Testcontainers vs. Postgres service container) — not yet decided
