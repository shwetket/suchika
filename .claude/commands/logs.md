# /logs — View Service Logs with lnav

Show, search, or filter runtime logs from Suchika services. Uses lnav (installed) to watch live log files in `~/.suchika/logs/`.

## Quick start

```powershell
# Load aliases first (if not already loaded)
. .\scripts\dev-aliases.ps1

# Open lnav watching all 5 services at once
lnav-dev

# Watch only specific services
lnav-dev wealth,gateway

# Or call the script directly
.\scripts\lnav.ps1 profile
```

Services write logs to `~/.suchika/logs/<service>.log` when running in dev mode (`quarkusDev`). Log files don't exist if the service hasn't been started.

---

## lnav Key Bindings

### Navigation
| Key | Action |
|---|---|
| `e` / `E` | Jump to next/prev **ERROR** line |
| `w` / `W` | Jump to next/prev **WARN** line |
| `g` / `G` | Go to start / end of log |
| `TAB` | Switch between open log files |
| `f` | Show field list for current log format |

### Searching
| Key / Command | Action |
|---|---|
| `/pattern` | Search forward (regex) |
| `?pattern` | Search backward (regex) |
| `n` / `N` | Next / previous match |
| `:filter-in <pattern>` | Show only lines matching pattern |
| `:filter-out <pattern>` | Hide lines matching pattern |
| `:reset-session` | Clear all filters |

### Time Navigation
| Key / Command | Action |
|---|---|
| `:goto <timestamp>` | Jump to a specific time, e.g. `:goto 12:05:00` |
| `:relative-goto -5m` | Go back 5 minutes |

---

## SQL Queries (semicolon mode)

Press `;` to enter SQL mode. All log fields are queryable.

### Useful queries for Suchika

```sql
-- Count errors per service in the last hour
SELECT service, count(*) as errors
FROM suchika_log
WHERE level = 'ERROR'
GROUP BY service ORDER BY errors DESC;

-- Show all ERROR and WARN lines from wealth domain
SELECT timestamp, level, body
FROM suchika_log
WHERE level IN ('ERROR', 'WARN') AND service = 'wealth'
ORDER BY timestamp;

-- Find all requests to a specific endpoint
SELECT timestamp, service, body
FROM suchika_log
WHERE body LIKE '%/api/v1/accounts%'
ORDER BY timestamp;

-- Count log lines per service (traffic overview)
SELECT service, level, count(*) as cnt
FROM suchika_log
GROUP BY service, level
ORDER BY service, level;

-- Find slow requests (if logged)
SELECT timestamp, service, body
FROM suchika_log
WHERE body LIKE '%ms%' AND body LIKE '%slow%';
```

---

## Filter Commands

```
# Show only errors and warnings
:filter-in ERROR|WARN

# Focus on one service (profile, wealth, health, household, gateway)
:filter-in \[wealth\]

# Hide Flyway noise during startup
:filter-out Flyway|HHH|hibernate

# Hide health-check polling noise  
:filter-out /q/health

# Reset all filters
:reset-session
```

---

## Log File Locations

| Service | Log file |
|---|---|
| profile | `~/.suchika/logs/profile.log` |
| wealth | `~/.suchika/logs/wealth.log` |
| health | `~/.suchika/logs/health.log` |
| household | `~/.suchika/logs/household.log` |
| gateway | `~/.suchika/logs/gateway.log` |

Rotation: 10 MB max, 2 backups. Old logs rotate to `<service>.log.1`, `<service>.log.2`.

---

## Useful Startup Filters

Run these right after opening lnav to reduce noise:

```
:filter-out io.quarkus
:filter-out org.hibernate
:filter-out org.flywaydb
:filter-out /q/health
```

This leaves only your application code log lines.
