# 001 - Scanner: manuell sökning med vald biljettyp ger inga träffar

> Status: Fixed  
> Datum: 2026-03-27  
> Plattformar: Android + iOS  
> Område: Scanner / manuell biljettsökning

## Problem

I manuell biljettsökning i scannern fungerar sökning när "Alla biljettyper" används, men om en specifik biljettyp väljs returneras inga träffar trots att matchande biljetter finns.

## Reproduktion

1. Öppna scanner för ett event med definierade biljettyper.
2. Öppna "Sök biljett manuellt".
3. Sök på e-post eller kod med filter "Alla biljettyper".
4. Verifiera att träffar visas.
5. Välj sedan en specifik biljettyp och sök igen med samma query.

Utfall: inga träffar.

## Rotorsak

Appen skickade biljettypens visningsnamn i API-filtret i stället för biljettypens ID.

- Backend-filtret för `ticketType` förväntar ID-värdet.
- "Alla" fungerar eftersom inget `ticketType`-filter skickas.
- Med en specifik typ blev filtret felaktigt och gav tomt resultat.

## Fix

Skicka `ticketTypeId` direkt i `ticketType`-fältet för filteranropet.

- Android: `ScannerViewModel.handleTicketSearch(...)`
- iOS: `ScannerViewModel.handleTicketSearch(...)`

## Verifiering

- Sökning med "Alla biljettyper" fungerar fortsatt.
- Sökning med specifik biljettyp returnerar nu förväntade träffar.
