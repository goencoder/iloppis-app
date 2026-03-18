# 003 - Scanner: manuell biljettsökning på e-post + biljettyp och bättre felinsyn

> **Status:** Förslag  
> **Datum:** 2026-03-18  
> **Plattformar:** Android + iOS  
> **Område:** Biljettskanner / entréflöde  

---

## Problem

I `iLoppis-app` fungerar manuell biljettinmatning idag bara om personalen känner till biljettens fulla id.

Det är sällan ett rimligt entréflöde:

- biljett-id är långt och svårt att skriva manuellt
- om man redan har hela id:t har man ofta också QR-koden
- entrépersonalen tänker oftast i termer av e-post och biljettyp, inte tekniskt id

Samtidigt blir felhanteringen i scannerflödet för tunn:

- backend returnerar ofta generiskt fel som `ticket not found or already scanned`
- appen kan i vissa fall hämta biljettinfo i efterhand, men den informationen leder inte till ett tydligt åtgärdsflöde
- detaljvyn från scanner har idag i praktiken bara `Stäng`

---

## Bekräftade nulägesfynd

### Nuvarande manuella flöde i appen

Android och iOS använder idag samma grundidé:

- användaren öppnar manuell inmatning
- skriver in biljettkod / biljett-id
- appen försöker tolka värdet som rått biljett-id eller JSON-payload
- appen anropar `scan` direkt

Det finns ingen manuell sökning på:

- e-post
- biljettyp
- status

### Appens nätverkslager använder idag bara två biljettanrop

I mobilappen finns idag bara:

- `scanTicket`
- `getTicket`

Appen exponerar alltså inte biljettfiltrering i sitt eget nätverkslager ännu.

Viktig säkerhetsnotering:

- appens verktygsnycklar mappar idag inte till en separat scanner-roll
- i backend får alla event-API-nycklar utom live stats rollen `cashier`
- `cashier` har i `permissions.yaml` bara tillgång till `ScanVisitorTicket`, inte `FilterVisitorTickets` eller `GetVisitorTicket`

Det betyder att vi inte kan "öppna lite biljettläsning för appen" genom att bara lägga fler metoder på `cashier` utan att samtidigt öppna dem för andra verktygsnycklar som delar samma roll.
Själva API-, authz- och migreringsbeslutet ska därför beskrivas i issue:n i `iloppis`, inte här.

### Backend har delar av stödet, men styrningen hör hemma i `iloppis`

Det viktiga fyndet är att backend redan stödjer:

- `FilterVisitorTickets`
- filter på `email`
- filter på `ticket_type`
- filter på `status`
- `free_text` som regex/contains över id, e-post och biljettyp

Dessutom tillåter valideringslagret att `event API key` använder `FilterVisitorTickets`, så scanner-nyckeln kan redan läsa biljettlistor för sitt event.

Men det räcker inte att endpointen finns.

Nuvarande authz-läge är:

- `FilterVisitorTickets`
  - grov auth: inte tillåten för rollen `cashier`
  - validering: tillåter `event API key`, men bara om `eventId` matchar nyckelns event
- `GetVisitorTicket`
  - grov auth: inte tillåten för rollen `cashier`
  - validering: tillåter inte idag `event API key` alls
- `ScanVisitorTicket`
  - grov auth: tillåten för `cashier`
  - validering: tillåter `event API key` om `eventId` matchar

Det betyder:

- inget nytt biljett-API behöver designas från scratch
- men backend authz behöver sannolikt justeras innan appen kan använda sök säkert
- riskanalys, rollsplit och eventuell migrering ska beskrivas i `iloppis`-issue:n

### Nuvarande felinsyn i appen

Vid HTTP-fel gör Android redan ett bra försök:

- appen hämtar biljett om den finns
- historiken sparar felscans
- felrader går att klicka upp

Men detaljdialogen visar idag bara detaljer och `Stäng`.

Det saknas alltså ett arbetsflöde för:

- hitta rätt biljett manuellt
- förstå varför skanningen misslyckades
- agera vidare från detaljvyn

---

## Mål

1. Gör e-postsökning till primärt manuellt entréflöde i scanner.
2. Låt användaren kombinera e-postsök med valfri biljettyp.
3. Gör det lättare att öppna biljettdetaljer efter fel eller sökträff.
4. Låt personalen markera en biljett som skannad direkt från detaljvyn när biljetten fortfarande är oskannad.
5. Behåll parity mellan Android och iOS.
6. Öppna inte mer biljettdata för verktygsnycklar än vad entréflödet faktiskt kräver.

---

## Implementeringsregler

Utvecklaren ska uttryckligen följa:

- `AGENTS.md`
- `.github/copilot-instructions.md`

Detta innebär bland annat:

- Android och iOS ska hållas funktionsmässigt paritetsjusterade om inte plattformsspecifik UX kräver en mindre avvikelse
- nya scannerflöden ska följa repo:ts Clean Architecture + MVVM + unidirectional data flow
- skärmar ska komponera UI från state och delegera affärslogik till ViewModel, inte bära egen business logic i vyn
- nya stateövergångar, actions/intents och sidoeffekter ska ägas i ViewModel/state-lagret, inte spridas i komponenter
- all användartext ska in i string resources / localized string files, inte hårdkodas i Android- eller iOS-vyer
- färger ska använda `AppColors`; inga hårdkodade färger får införas
- styling ska hållas i theme-/komponentlager och inte inline i skärmkod
- nya nätverksanrop, DTO:er och mappare ska följa befintlig network/domain-struktur
- om en ny komponent eller dialog införs ska motsvarande struktur finnas på båda plattformar där funktionen stöds

---

## Föreslagen design

## Säkerhetsprincip

Mobilappen arbetar här som verktyg med event-API-nyckel, inte som inloggad användare.

Därför gäller:

- vi får inte öppna generella biljett-read-API:er för alla verktygsnycklar av bekvämlighetsskäl
- varje öppnat API måste vara bundet till samma `eventId` som nyckeln tillhör
- vi ska helst särskilja scanner-nycklar från övriga verktygsnycklar innan vi öppnar fler biljettläsande metoder

Rekommendation:

- separera scanner från generisk `cashier`
- låt backend/authz-beslutet dokumenteras och ägas i `iloppis`

Utan en sådan uppdelning betyder varje nytt biljett-read-API på `cashier` i praktiken att även kassanycklar får biljettläsning.

## 1. Ersätt primär manuell inmatning med manuell sökning

Nuvarande CTA:

- `Ange biljettkod`

Föreslagen primär CTA:

- `Sök biljett manuellt`

Innehåll i sökdialog/sheet:

- e-postfält
- biljettyp-dropdown, default `Alla`
- sökknapp

Sökstrategi:

- använd `freeText` för e-postsök så att `contains` och prefix fungerar
- använd `ticketType` som separat exakt filter när användaren väljer en typ
- filtrera gärna till oskannade som default om det förbättrar entréflödet, men visa tydligt om filtret används

Rekommendation:

- behåll rå biljett-id-inmatning som sekundärt supportläge, inte som primärt flöde

## 2. Visa sökträffar som en riktig resultatlista

Varje träff bör visa:

- biljettyp
- e-post
- status
- giltig från/till
- eventnamn om relevant

Tryck på en träff:

- öppnar biljettdetalj

Primär action på raden:

- `Markera som skannad` om status är oskannad

## 3. Förbättra detaljvyn för fel och manuella träffar

När en biljett går att hämta ska detaljvyn inte bara vara läsbar, utan användbar.

Föreslagen ordning i detaljvyn:

1. status
2. biljettyp
3. e-post
4. primära actions
5. övriga fält, inklusive biljett-id

Föreslagna actions:

- `Markera som skannad` om `status != SCANNED`
- `Stäng`

Detta ska gälla både när användaren:

- öppnar en sökträff
- klickar på ett fel i historiken

## 4. Gör fel mer begripliga

Nuvarande backend-fel `ticket not found or already scanned` är för generiskt som slutlig UX.

Appen bör därför:

1. fortsätta försöka hämta biljettdata efter fel när det är möjligt
2. klassificera utfallet bättre i UI
3. visa biljettstatus och detaljer om biljett finns

Exempel:

- finns biljett och status är skannad: visa `Redan skannad` + detaljer
- finns biljett och status är oskannad men scan misslyckades av annan anledning: visa detaljvyn och låt användaren försöka igen
- finns ingen biljett: visa tydligt `Biljett hittades inte`

---

## Lager och påverkan

### Presentation: krävs

Berör sannolikt:

- Android `ScannerScreen.kt`
- Android `ScannerViewModel.kt`
- iOS motsvarande scanner screen/view model
- nya komponenter för sökdialog/resultatlista

### Network/data: krävs

Appen behöver lägga till klientstöd för befintliga backend-API:er:

- `POST /v1/events/{eventId}/visitor_tickets:filter`
- eventuellt `GET /v1/events/{eventId}/visitor_tickets/{id}` om filtersvaret inte räcker för detaljvyn

Detta bör in i:

- Android `VisitorAPI`
- iOS `ApiClient`
- ev. mappers för biljettlista om separata DTO:er behövs

### Backend/authz: refereras, men ägs i `iloppis`

Backend har redan delar av det funktionella stödet, men all förändring av:

- roller
- `permissions.yaml`
- valideringsregler
- migrering av befintliga verktygsnycklar

ska beskrivas och implementeras via issue i `iloppis`.

Om vi senare vill ha särskild "force scan"-funktion är det en separat backendfråga, men den behövs inte för denna issue.

---

## Acceptanskriterier

- [ ] Scanner har ett manuellt sökflöde baserat på e-post i stället för att kräva biljett-id som primär väg.
- [ ] Användaren kan begränsa sökning med biljettyp.
- [ ] Sökresultat visar flera träffar i en lista.
- [ ] Tryck på träff öppnar biljettdetalj.
- [ ] Biljettdetalj från scanner kan markera oskannad biljett som skannad.
- [ ] Felhistorik kan fortfarande öppnas, men ger mer hjälpsam detaljinformation.
- [ ] Android och iOS följer samma flöde och yta.
- [ ] Klientlösningen förutsätter inte bredare verktygsbehörighet än vad backend-issue:n i `iloppis` beslutat.

---

## Testning

### Manuell testning

1. Öppna scanner i Android och iOS.
2. Välj manuell sökning.
3. Sök på del av e-postadress som matchar flera biljetter.
4. Filtrera sedan samma sökning till en specifik biljettyp, till exempel `Entrébiljett 14.00 - Barnvagn tillåtet`.
5. Öppna en träff och verifiera att detaljvyn visar status, biljettyp och e-post.
6. Markera en oskannad biljett som skannad från detaljvyn.
7. Skanna samma biljett igen och verifiera att felresultatet visar `Redan skannad` och öppningsbar detalj.
8. Sök på e-post som inte finns och verifiera tydligt tomt läge.
9. Verifiera backend-beteenden enligt authz-testfall i `iloppis`-issue:n innan klientflödet anses klart.

### Nytt UI-testfall

`scanner.manual-search-by-email-and-ticket-type`

Scenario:

1. Öppna scanner.
2. Öppna manuell sökning.
3. Skriv in del av e-post.
4. Välj biljettyp.
5. Verifiera att resultatlistan filtreras korrekt.
6. Öppna en träff.
7. Verifiera att `Markera som skannad` finns för oskannad biljett.
8. Kör action och verifiera att status uppdateras.

---

## Definition of Done

- [ ] Filterendpoint kopplad i appens nätverkslager.
- [ ] Nytt manuellt sökflöde implementerat.
- [ ] Detaljvyn stöder entréåtgärd från sök/felresultat.
- [ ] Android och iOS är funktionellt paritetsjusterade.
- [ ] Implementationen följer uttryckligen reglerna i `AGENTS.md` och `.github/copilot-instructions.md`.
- [ ] Inga hårdkodade färger eller hårdkodad användartext har introducerats; `AppColors` och lokaliserade strängresurser används.
- [ ] Nytt UI-testfall implementerat för respektive plattform eller testlager där det hör hemma.
- [ ] Nytt UI-testfall kört och grönt.
- [ ] Android bygger med `./gradlew assembleDebug`.
- [ ] iOS bygger med `xcodebuild -scheme iLoppis -destination 'platform=iOS Simulator,name=iPhone 15' build` eller motsvarande uppdaterad simulator-destination.
