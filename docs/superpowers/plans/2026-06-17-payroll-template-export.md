# Payroll Template Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a payroll template generation system for labor companies, with isolated user data, admin oversight, editable payroll batches, bank Excel export, and operation logs.

**Architecture:** Create a Spring Boot 3 backend and Vue 3 + Element Plus frontend in this workspace. Payroll batches store recipient and amount data only; each export chooses a paying unit and bank template, then writes a new export record and Excel file without mutating the batch.

**Tech Stack:** Spring Boot 3, Java 17, MySQL 8, MyBatis Plus, Spring Security JWT, Apache POI, Vue 3, Vite, TypeScript, Element Plus, Pinia, Axios.

---

## File Structure

Backend root: `server/`

- `server/pom.xml`: backend dependencies.
- `server/src/main/java/com/lingdong/payroll/PayrollApplication.java`: Spring Boot entrypoint.
- `server/src/main/java/com/lingdong/payroll/common/`: common response, exceptions, audit helpers.
- `server/src/main/java/com/lingdong/payroll/security/`: JWT, login, RBAC, current user context.
- `server/src/main/java/com/lingdong/payroll/domain/`: entities, mappers, services, controllers by module.
- `server/src/main/resources/db/migration/`: SQL schema migrations.
- `server/src/main/resources/templates/bank/`: bank Excel templates copied from workspace.
- `server/src/test/java/com/lingdong/payroll/`: backend tests.

Frontend root: `web/`

- `web/package.json`: frontend dependencies.
- `web/src/main.ts`: Vue entrypoint.
- `web/src/router/`: routes and route guards.
- `web/src/stores/`: auth and user state.
- `web/src/api/`: API clients.
- `web/src/views/`: pages.
- `web/src/components/`: shared table, form, and dialog components.
- `web/src/styles/`: theme and layout styling.

Docs:

- `docs/superpowers/specs/2026-06-17-payroll-template-export-design.md`: approved design.
- `docs/superpowers/plans/2026-06-17-payroll-template-export.md`: this plan.

---

### Task 1: Initialize Backend Project

**Files:**
- Create: `server/pom.xml`
- Create: `server/src/main/java/com/lingdong/payroll/PayrollApplication.java`
- Create: `server/src/main/resources/application.yml`
- Create: `server/src/test/java/com/lingdong/payroll/PayrollApplicationTests.java`

- [ ] **Step 1: Create Spring Boot project files**

Create `server/pom.xml` with Java 17, Spring Web, Spring Security, Validation, MyBatis Plus, MySQL, Flyway, Apache POI, Lombok, and test dependencies.

- [ ] **Step 2: Add application entrypoint**

Create `PayrollApplication.java`:

```java
package com.lingdong.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PayrollApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayrollApplication.class, args);
    }
}
```

- [ ] **Step 3: Add base config**

Create `application.yml` with profiles for local development:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lingdong_payroll?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
  flyway:
    enabled: true
    locations: classpath:db/migration

payroll:
  jwt-secret: change-this-in-production
  export-dir: ./data/exports
```

- [ ] **Step 4: Add smoke test**

Create `PayrollApplicationTests.java`:

```java
package com.lingdong.payroll;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PayrollApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 5: Verify backend boots in tests**

Run: `mvn -f server/pom.xml test`

Expected: tests pass.

- [ ] **Step 6: Commit**

Run:

```bash
git add server
git commit -m "chore: initialize payroll backend"
```

---

### Task 2: Add Database Schema

**Files:**
- Create: `server/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `server/src/test/java/com/lingdong/payroll/schema/SchemaMigrationTest.java`

- [ ] **Step 1: Create migration**

Create tables:

- `sys_user`
- `sys_role`
- `sys_user_role`
- `payee_person`
- `paying_unit`
- `payroll_batch`
- `payroll_batch_item`
- `export_record`
- `operation_log`

Important constraints:

```sql
create unique index uk_payee_created_id_card on payee_person(created_by, id_card_no);
create unique index uk_unit_created_bank_account on paying_unit(created_by, bank_account);
create index idx_batch_created_by on payroll_batch(created_by);
create index idx_export_created_by on export_record(created_by);
create index idx_log_operator_time on operation_log(operator_id, operated_at);
```

- [ ] **Step 2: Seed roles and admin**

Insert roles:

```sql
insert into sys_role(code, name) values ('ADMIN', '管理员'), ('OPERATOR', '录入人员');
```

Insert one local admin user with a BCrypt password generated during implementation.

- [ ] **Step 3: Add migration test**

Create a test that starts the Spring context against a test database and asserts the Flyway migration succeeds.

- [ ] **Step 4: Verify schema**

Run: `mvn -f server/pom.xml test`

Expected: schema migration test passes.

- [ ] **Step 5: Commit**

Run:

```bash
git add server/src/main/resources/db/migration server/src/test
git commit -m "feat: add payroll database schema"
```

---

### Task 3: Implement Auth, RBAC, and Data Scope

**Files:**
- Create: `server/src/main/java/com/lingdong/payroll/security/JwtService.java`
- Create: `server/src/main/java/com/lingdong/payroll/security/SecurityConfig.java`
- Create: `server/src/main/java/com/lingdong/payroll/security/CurrentUser.java`
- Create: `server/src/main/java/com/lingdong/payroll/security/CurrentUserProvider.java`
- Create: `server/src/main/java/com/lingdong/payroll/security/DataScope.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/auth/AuthController.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/auth/dto/LoginRequest.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/auth/dto/LoginResponse.java`
- Test: `server/src/test/java/com/lingdong/payroll/security/DataScopeTest.java`

- [ ] **Step 1: Define current user model**

`CurrentUser` must expose:

```java
public record CurrentUser(Long id, String username, Set<String> roleCodes) {
    public boolean isAdmin() {
        return roleCodes.contains("ADMIN");
    }
}
```

- [ ] **Step 2: Implement data-scope helper**

`DataScope` must return no creator filter for admins and `created_by = currentUser.id()` for ordinary operators.

- [ ] **Step 3: Write data-scope tests**

Test that admin scope is unrestricted and operator scope contains only their own user ID.

- [ ] **Step 4: Implement JWT login**

`POST /api/auth/login` accepts username and password, returns token, user ID, username, and role codes.

- [ ] **Step 5: Protect APIs**

Configure `/api/auth/login` public and all other `/api/**` routes authenticated.

- [ ] **Step 6: Verify**

Run: `mvn -f server/pom.xml test`

Expected: auth and data-scope tests pass.

- [ ] **Step 7: Commit**

Run:

```bash
git add server/src/main/java/com/lingdong/payroll/security server/src/main/java/com/lingdong/payroll/domain/auth server/src/test
git commit -m "feat: add auth and data scope"
```

---

### Task 4: Implement Payee Person Library

**Files:**
- Create: `server/src/main/java/com/lingdong/payroll/domain/person/PayeePerson.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/person/PayeePersonMapper.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/person/PayeePersonService.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/person/PayeePersonController.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/person/dto/PayeePersonUpsertRequest.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/person/dto/PayeePersonResponse.java`
- Test: `server/src/test/java/com/lingdong/payroll/domain/person/PayeePersonServiceTest.java`

- [ ] **Step 1: Write upsert tests**

Test cases:

- Creating a person requires `name`, `idCardNo`, `bankAccount`, and `accountName`.
- Same creator + same ID card updates the existing record.
- Different creators can each have their own record with the same ID card.
- Admin list can see all records; operator list sees own records only.

- [ ] **Step 2: Implement entity and mapper**

Map columns for name, ID card, phone, bank account, account name, bank name, bank type, bank category, CNAPS number, created_by, timestamps.

- [ ] **Step 3: Implement service**

`upsertByIdCard(request, currentUser)`:

- Find by `created_by + id_card_no`.
- Insert if not found.
- Update mutable fields if found.
- Write operation log for create or update.

- [ ] **Step 4: Implement controller**

Routes:

- `GET /api/persons?keyword=`
- `POST /api/persons`
- `PUT /api/persons/{id}`
- `DELETE /api/persons/{id}`

- [ ] **Step 5: Verify**

Run: `mvn -f server/pom.xml test -Dtest=PayeePersonServiceTest`

Expected: tests pass.

- [ ] **Step 6: Commit**

Run:

```bash
git add server/src/main/java/com/lingdong/payroll/domain/person server/src/test/java/com/lingdong/payroll/domain/person
git commit -m "feat: add payee person library"
```

---

### Task 5: Implement Paying Unit Library

**Files:**
- Create: `server/src/main/java/com/lingdong/payroll/domain/unit/PayingUnit.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/unit/PayingUnitMapper.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/unit/PayingUnitService.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/unit/PayingUnitController.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/unit/dto/PayingUnitUpsertRequest.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/unit/dto/PayingUnitResponse.java`
- Test: `server/src/test/java/com/lingdong/payroll/domain/unit/PayingUnitServiceTest.java`

- [ ] **Step 1: Write upsert tests**

Test cases:

- Creating a unit requires bank account and account name.
- Same creator + same bank account updates the existing unit.
- Different creators can each have their own unit with the same bank account.
- Admin list can see all records; operator list sees own records only.

- [ ] **Step 2: Implement entity, mapper, service, and controller**

Routes:

- `GET /api/units?keyword=`
- `POST /api/units`
- `PUT /api/units/{id}`
- `DELETE /api/units/{id}`

- [ ] **Step 3: Verify**

Run: `mvn -f server/pom.xml test -Dtest=PayingUnitServiceTest`

Expected: tests pass.

- [ ] **Step 4: Commit**

Run:

```bash
git add server/src/main/java/com/lingdong/payroll/domain/unit server/src/test/java/com/lingdong/payroll/domain/unit
git commit -m "feat: add paying unit library"
```

---

### Task 6: Implement Payroll Batch Editing

**Files:**
- Create: `server/src/main/java/com/lingdong/payroll/domain/batch/PayrollBatch.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/batch/PayrollBatchItem.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/batch/PayrollBatchMapper.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/batch/PayrollBatchItemMapper.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/batch/PayrollBatchService.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/batch/PayrollBatchController.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/batch/dto/PayrollBatchSaveRequest.java`
- Test: `server/src/test/java/com/lingdong/payroll/domain/batch/PayrollBatchServiceTest.java`

- [ ] **Step 1: Write batch tests**

Test cases:

- Saving a batch calculates total people and total amount.
- Saving a manually entered person automatically upserts into the person library by ID card.
- Existing person rows do not duplicate person records.
- Updating an exported batch is allowed and creates an operation log.
- Operator cannot access another operator's batch; admin can.

- [ ] **Step 2: Implement save request**

Request shape:

```json
{
  "batchName": "2026年6月工资",
  "payDate": "2026-06-30",
  "defaultSummary": "工资",
  "remark": "6月劳务工资",
  "items": [
    {
      "personId": 1,
      "name": "张三",
      "idCardNo": "500000199001010000",
      "phone": "13800000000",
      "bankAccount": "6222000000000000",
      "accountName": "张三",
      "bankName": "中国建设银行重庆分行",
      "bankType": "建设银行",
      "bankCategory": "本行",
      "cnapsNo": "",
      "amount": 5000.00,
      "summary": "工资",
      "remark": "6月工资"
    }
  ]
}
```

- [ ] **Step 3: Implement service**

Rules:

- Validate required fields.
- Upsert missing/manual people by ID card.
- Replace current batch items with submitted items inside one transaction.
- Recalculate totals using `BigDecimal`.
- Keep batch independent from paying unit and bank template.
- Write logs for create, update, delete item, and update item.

- [ ] **Step 4: Implement controller**

Routes:

- `GET /api/batches?keyword=&page=&size=`
- `GET /api/batches/{id}`
- `POST /api/batches`
- `PUT /api/batches/{id}`
- `DELETE /api/batches/{id}`

- [ ] **Step 5: Verify**

Run: `mvn -f server/pom.xml test -Dtest=PayrollBatchServiceTest`

Expected: tests pass.

- [ ] **Step 6: Commit**

Run:

```bash
git add server/src/main/java/com/lingdong/payroll/domain/batch server/src/test/java/com/lingdong/payroll/domain/batch
git commit -m "feat: add payroll batch editing"
```

---

### Task 7: Implement Bank Excel Export

**Files:**
- Copy: `建设银行工资模版.xlsx` to `server/src/main/resources/templates/bank/ccb-payroll.xlsx`
- Copy: `中国银行工资模版.xls` to `server/src/main/resources/templates/bank/boc-payroll.xls`
- Create: `server/src/main/java/com/lingdong/payroll/domain/export/BankTemplateType.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/export/PayrollExportService.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/export/ExportRecord.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/export/ExportRecordMapper.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/export/ExportController.java`
- Test: `server/src/test/java/com/lingdong/payroll/domain/export/PayrollExportServiceTest.java`

- [ ] **Step 1: Write export tests**

Test cases:

- CCB export writes headers and rows to `.xlsx`.
- BOC export writes headers and rows to `.xls`.
- Export creates a new export record every time.
- Export does not modify payroll batch or batch items.
- Export validates missing bank account, account name, amount, and required CNAPS for BOC other-bank rows.

- [ ] **Step 2: Implement template enum**

Values:

```java
public enum BankTemplateType {
    CCB("建设银行", "templates/bank/ccb-payroll.xlsx"),
    BOC("中国银行", "templates/bank/boc-payroll.xls");
}
```

- [ ] **Step 3: Implement CCB mapping**

Write rows starting at row 2:

- Column A: sequence number
- Column B: bank account
- Column C: account name
- Column D: amount
- Column E: same-bank flag, `0` for 建设银行, otherwise `1`
- Column F: bank name
- Column G: CNAPS number
- Column H: summary
- Column I: remark

- [ ] **Step 4: Implement BOC mapping**

Write rows starting at row 2:

- Column A: bank account
- Column B: account name
- Column C: amount
- Column D: `中行` for 中国银行, otherwise `他行`
- Column E: CNAPS number
- Column F: `居民身份证`
- Column G: ID card number
- Column H: summary
- Column I: remark

- [ ] **Step 5: Implement export API**

`POST /api/exports`

Request:

```json
{
  "batchId": 1,
  "payingUnitId": 1,
  "templateType": "CCB"
}
```

Response includes export record ID, file name, total count, total amount, and download URL.

- [ ] **Step 6: Implement download API**

`GET /api/exports/{id}/download`

Rules:

- Operator can download own export only.
- Admin can download all exports.

- [ ] **Step 7: Verify**

Run: `mvn -f server/pom.xml test -Dtest=PayrollExportServiceTest`

Expected: tests pass and generated test files can be opened by Apache POI.

- [ ] **Step 8: Commit**

Run:

```bash
git add server/src/main/resources/templates/bank server/src/main/java/com/lingdong/payroll/domain/export server/src/test/java/com/lingdong/payroll/domain/export
git commit -m "feat: add bank excel export"
```

---

### Task 8: Implement Operation Log Query

**Files:**
- Create: `server/src/main/java/com/lingdong/payroll/domain/log/OperationLog.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/log/OperationLogMapper.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/log/OperationLogService.java`
- Create: `server/src/main/java/com/lingdong/payroll/domain/log/OperationLogController.java`
- Test: `server/src/test/java/com/lingdong/payroll/domain/log/OperationLogServiceTest.java`

- [ ] **Step 1: Write log tests**

Test cases:

- Business service can write create/update/delete/export logs.
- Log stores before and after JSON text.
- Admin can query all logs.
- Operator can query only own operation logs.

- [ ] **Step 2: Implement log writer**

Expose:

```java
void record(Long operatorId, String action, String module, Long businessId, Object beforeValue, Object afterValue);
```

- [ ] **Step 3: Implement log list API**

`GET /api/logs?module=&action=&operator=&startDate=&endDate=&page=&size=`

- [ ] **Step 4: Verify**

Run: `mvn -f server/pom.xml test -Dtest=OperationLogServiceTest`

Expected: tests pass.

- [ ] **Step 5: Commit**

Run:

```bash
git add server/src/main/java/com/lingdong/payroll/domain/log server/src/test/java/com/lingdong/payroll/domain/log
git commit -m "feat: add operation log query"
```

---

### Task 9: Initialize Frontend Project

**Files:**
- Create: `web/package.json`
- Create: `web/vite.config.ts`
- Create: `web/src/main.ts`
- Create: `web/src/App.vue`
- Create: `web/src/router/index.ts`
- Create: `web/src/stores/auth.ts`
- Create: `web/src/api/http.ts`
- Create: `web/src/styles/theme.css`

- [ ] **Step 1: Create Vite Vue app**

Use Vue 3, TypeScript, Element Plus, Pinia, Axios, and Vue Router.

- [ ] **Step 2: Add HTTP client**

Axios client must attach JWT token and redirect to login on 401.

- [ ] **Step 3: Add routes**

Routes:

- `/login`
- `/persons`
- `/units`
- `/batches`
- `/batches/new`
- `/batches/:id`
- `/exports`
- `/logs`

- [ ] **Step 4: Add app layout**

Use a left menu and top user bar. Keep the layout dense, clean, and suitable for repeated back-office use.

- [ ] **Step 5: Verify**

Run:

```bash
cd web
npm install
npm run build
```

Expected: frontend builds.

- [ ] **Step 6: Commit**

Run:

```bash
git add web
git commit -m "chore: initialize payroll frontend"
```

---

### Task 10: Build Frontend Pages

**Files:**
- Create: `web/src/views/persons/PersonList.vue`
- Create: `web/src/views/units/UnitList.vue`
- Create: `web/src/views/batches/BatchList.vue`
- Create: `web/src/views/batches/BatchEditor.vue`
- Create: `web/src/views/batches/PersonPickerDialog.vue`
- Create: `web/src/views/exports/ExportList.vue`
- Create: `web/src/views/logs/OperationLogList.vue`
- Create: `web/src/api/persons.ts`
- Create: `web/src/api/units.ts`
- Create: `web/src/api/batches.ts`
- Create: `web/src/api/exports.ts`
- Create: `web/src/api/logs.ts`

- [ ] **Step 1: Build person and unit library pages**

Each page supports search, create, edit, delete, and admin-visible creator column.

- [ ] **Step 2: Build batch list**

Show batch name, pay date, total people, total amount, creator, updated time, and actions.

- [ ] **Step 3: Build person picker dialog**

Search by name, ID card, phone, or bank account. Support multi-select and return selected rows.

- [ ] **Step 4: Build batch editor**

Required behavior:

- Top form for batch name, pay date, default summary, remark.
- Toolbar with batch select people, add row, delete selected rows, batch set summary, save, export.
- Editable detail table for recipient data.
- Amount column right-aligned.
- Real-time summary cards for total people, total amount, missing bank account count, missing CNAPS count.
- Row-level validation messages.

- [ ] **Step 5: Build export dialog**

From batch editor, open dialog to choose paying unit and bank template. Submit to `/api/exports`, then show download link.

- [ ] **Step 6: Build export and log pages**

Export page supports list and download. Log page supports module, action, operator, and date filters.

- [ ] **Step 7: Verify**

Run:

```bash
cd web
npm run build
```

Expected: frontend builds.

- [ ] **Step 8: Commit**

Run:

```bash
git add web/src
git commit -m "feat: add payroll frontend pages"
```

---

### Task 11: Polish UI and End-to-End Verification

**Files:**
- Modify: `web/src/styles/theme.css`
- Modify: `web/src/views/batches/BatchEditor.vue`
- Create: `docs/qa/payroll-template-export-checklist.md`

- [ ] **Step 1: Polish visual design**

Apply:

- Restrained blue or teal primary color.
- Neutral page background.
- Clean table density.
- Clear amount typography.
- No marketing hero sections.
- No nested cards.
- Visible but calm validation states.

- [ ] **Step 2: Create QA checklist**

Checklist must cover:

- Operator sees only own people, units, batches, exports, and logs.
- Admin sees all data.
- Batch total people and total amount update immediately.
- Manual person entry saves to person library.
- Manual paying unit entry saves to unit library.
- CCB export matches `建设银行工资模版.xlsx` columns.
- BOC export matches `中国银行工资模版.xls` columns.
- Export history creates one record per export.
- Batch remains editable after export.
- Operation logs are written for create, update, delete, export, and admin adjustment.

- [ ] **Step 3: Run backend tests**

Run: `mvn -f server/pom.xml test`

Expected: all backend tests pass.

- [ ] **Step 4: Run frontend build**

Run:

```bash
cd web
npm run build
```

Expected: frontend build passes.

- [ ] **Step 5: Manual browser verification**

Start backend and frontend:

```bash
mvn -f server/pom.xml spring-boot:run
cd web
npm run dev
```

Open the frontend, log in as admin and operator, then run through the QA checklist.

- [ ] **Step 6: Commit**

Run:

```bash
git add web docs/qa
git commit -m "polish: verify payroll template workflow"
```

---

## Self-Review

Spec coverage:

- Personnel library: covered by Task 4 and Task 10.
- Paying unit library: covered by Task 5 and Task 10.
- Payroll batch editing: covered by Task 6 and Task 10.
- Automatic total amount: covered by Task 6 backend totals and Task 10 frontend real-time cards.
- Bank exports: covered by Task 7.
- Data isolation and admin access: covered by Task 3 and each service test.
- Operation logs: covered by Task 8.
- UI beauty and usability: covered by Task 10 and Task 11.
- Out-of-scope items remain excluded: no bank submission,回盘, approval flow, project/team grouping, or finance reports.

Placeholder scan:

- No task depends on undefined future work.
- No step uses banned placeholder markers.

Type consistency:

- Backend modules consistently use person, unit, batch, export, and log domain names.
- Frontend routes and API files match backend route names.
