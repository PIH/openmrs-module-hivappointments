# HIV Appointments Module

Backend for the `@pih/esm-hiv-appointments-app` O3 frontend. Lists HIV clinical appointments and ARV pickups by site,
computed from the return visit date obs (`PIH:5096`), and records follow-up actions (contact attempts, rescheduling).

## How an appointment is computed

Every non-voided return visit date obs on an HIV encounter is an appointment, for patients enrolled in the HIV program
(`b1cb1fc1-…`) and alive on the scheduled date. The site is the program enrollment location, or the encounter location.

| Category   | Scheduled by (and honored by)                                               |
|------------|------------------------------------------------------------------------------|
| CLINICAL   | HIV Intake, HIV Followup, EID Followup, PMTCT Intake, PMTCT Followup        |
| ARV_PICKUP | HIV drug dispensing                                                          |

Status: `HONORED` / `HONORED_LATE` if a visit of the category happened after the scheduling encounter;
otherwise `RESCHEDULED` if a newer date was recorded; otherwise `UPCOMING`, `MISSED`, or `LTFU` beyond
`hivappointments.ltfuThresholdDays` (28, as in `statusData/hiv/hivPatientStatus.sql`).

Follow-up actions are saved as encounters of type *HIV Appointment Tracking* / *HIV ARV Pickup Tracking*
(created on startup), with a new return visit date obs and/or a comment obs (`CIEL:161011`).

## REST API

- `GET /ws/rest/v1/hivappointments?fromDate=2026-09-01&toDate=2026-09-30&location=<uuid>&category=CLINICAL&status=MISSED,LTFU&q=<name or id>&startIndex=0&limit=50`
- `POST /ws/rest/v1/hivappointments/tracking` `{ "appointment": "<obs uuid>", "newDate": "2026-10-05", "comment": "...", "location": "<uuid>" }`
  returns `409` if the appointment was rescheduled or honored meanwhile
- `GET /ws/rest/v1/hivappointments/config`

Privileges: `View HIV Appointments`, `Manage HIV Appointments`. All metadata references are global properties (see `config.xml`).

## Build

    mvn clean package   # omod/target/hivappointments-<version>.omod
