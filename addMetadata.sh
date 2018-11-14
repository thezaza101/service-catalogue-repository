#!/bin/sh


#id: "5b3eb4a47d0e99000457ffaa",
#name: "Superannuation Dashboard",
curl -X POST $2/metadata/5b3eb4a47d0e99000457ffaa --user $1 -H "Content-Type: application/json" --data '{"agency":"ato.gov.au","space":"super"}'


#id: "5b3eb49c7d0e99000457ffa9",
#name: "National Personal Insolvency Index Search",
curl -X POST $2/metadata/5b3eb49c7d0e99000457ffa9 --user $1 -H "Content-Type: application/json" --data '{"agency":"afsa.gov.au","space":"afsa"}'


#id: "5b3eb4917d0e99000457ffa8",
#name: "Debt Agreements Service",
curl -X POST $2/metadata/5b3eb4917d0e99000457ffa8 --user $1 -H "Content-Type: application/json" --data '{"agency":"afsa.gov.au","space":"afsa"}'


#id: "5bd921345149e90004f326a7",
#name: "Common Government Branding",
curl -X POST $2/metadata/5bd921345149e90004f326a7 --user $1 -H "Content-Type: application/json" --data '{"agency":"ato.gov.au","space":"apigovau"}'


#id: "5b3eb4707d0e99000457ffa5",
#name: "Definitions Catalogue ",
curl -X POST $2/metadata/5b3eb4707d0e99000457ffa5 --user $1 -H "Content-Type: application/json" --data '{"agency":"ato.gov.au","space":"apigovau"}'


#id: "5b639f0f63f18432cd0e1a66",
#name: "ABN Lookup",
curl -X POST $2/metadata/5b639f0f63f18432cd0e1a66 --user $1 -H "Content-Type: application/json" --data '{"agency":"ato.gov.au","space":"abr"}'


#id: "5b3eb4857d0e99000457ffa7",
#name: "Digital Capability Locator Service",
curl -X POST $2/metadata/5b3eb4857d0e99000457ffa7 --user $1 -H "Content-Type: application/json" --data '{"agency":"ato.gov.au","space":"eInvoicing"}'


#id: "5b3eb47b7d0e99000457ffa6",
#name: "ANZSIC code search",
curl -X POST $2/metadata/5b3eb47b7d0e99000457ffa6 --user $1 -H "Content-Type: application/json" --data '{"agency":"abs.gov.au","space":"anzsic"}'

