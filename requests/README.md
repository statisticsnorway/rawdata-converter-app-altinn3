## Starte en ny konverteringsjobb

Følgende gir en kort beskrivelse av hvordan du kan starte en ny converter-jobb.

## Kubectl

Gitt at du har konfigurert kubectl på din maskin (snakk med BIP om tilgang og assistanse),
kan du opprette en port-forward tunnel til converter-podden.

Sørg for at din kubectl config peker på riktig miljø (staging). Tips: benytt verktøyet kubectx for å switche mellom
aktive miljøer og namespaces:
```sh
kubectx
```

Følgende gir en liste over alle kjørende podder
```sh
kubectl get pods 
```

Det kan være lurt å assigne navnet på podden til en en env-variable, f.eks `$POD`:
```sh
POD=$(kubectl get pods -o=name | grep rawdata-converter-altinn3)
```

Port-forward fra din lokale maskin (port 18080) til podden (port 8080):
```sh
kubectl port-forward $POD 30900:8080
```

## Opprett en ny converter jobb

Gitt at du har konfiguert en port-forward tunnel som beskrevet ovenfor, har du nå mulighet til å gjøre kall til
REST-tjenestene som er tilgjengelig på den kjørende converter-instansen.

Følgende viser hvordan du kan starte en jobb med curl:

```sh
curl --request POST 'http://localhost:18080/jobs' \
--header 'Content-Type: application/json' \
--data '{
  "jobConfig": {
    "parent": "base",
    "dryrun": false,
    "rawdataSource": {
      "topic": "2020-11-19-altinn-ra0678-test",
      "initialPosition": "FIRST"
    },
    "targetDataset": {
      "publishMetadata": true
    },
    "targetStorage": {
      "path": "/kilde/altinn/3/test/ra0678m/4664/45918/20201119",
      "version": 1604071041000
    }
  },
  "converterConfig": {
    "schemaProps": {
      "dataType": "RA-0678_M",
      "dataFormatProvider": "SERES",
      "dataFormatId": "4664",
      "dataFormatVersion": "45918"
    }
  }
}'
```

Det kan være lurt å gjøre en "dryrun" (`dryrun=true`) første gang. Da vil du kun simulere en konvertering uten at data skrives.
Dersom du ikke ønsker at datasettet skal bli tilgjengelig med det samme kan du sette `publishMetadata=false`.

Du kan sjekke status for jobber slik:

```sh
curl http://localhost:30900/jobs/execution-summary
```

Det er ligger noen eksempel-requests for ulike topics [her](test-requests.http).
Dersom du har IntelliJ kan du benytte den innebygde http-klienten (tilsvarende "Postman")
for å eksekvere disse direkte fra IntelliJ.


## Logging
Det kan være nyttig å se loggene som podden produserer. Disse kan du se via [Kibana](https://kibana.staging-bip-app.ssb.no/app/kibana#/discover/92b7bc60-3305-11eb-9cdb-0d1371e5ba95?_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-1h,to:now))&_a=(columns:!(level,message),filters:!(('$state':(store:appState),meta:(alias:rawdata-converter-altinn3,disabled:!f,index:'92e6e0f0-433a-11ea-b3fb-135908c6f85f',key:kubernetes.container_name,negate:!f,params:(query:rawdata-converter-altinn3),type:phrase,value:rawdata-converter-altinn3),query:(match:(kubernetes.container_name:(query:rawdata-converter-altinn3,type:phrase)))),('$state':(store:appState),meta:(alias:!n,disabled:!f,index:'92e6e0f0-433a-11ea-b3fb-135908c6f85f',key:level,negate:!f,params:!(INFO,WARN,ERROR),type:phrases,value:'INFO,%20WARN,%20ERROR'),query:(bool:(minimum_should_match:1,should:!((match_phrase:(level:INFO)),(match_phrase:(level:WARN)),(match_phrase:(level:ERROR))))))),index:'92e6e0f0-433a-11ea-b3fb-135908c6f85f',interval:auto,query:(language:kuery,query:''),sort:!(!('@timestamp',desc)))),
eller du kan lytte på loggene på podden direkte, f.eks slik:

```sh
kc logs $POD -c rawdata-converter-altinn3-cont --follow
```
