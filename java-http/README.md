# Java-http module

## OphHttpClient

### Usage
Some examples

#### Put with an error handler and without return value
    OphHttpRequest request = OphHttpRequest.Builder
            .put(urlConfiguration.url("oppijanumerorekisteri-service.s2s.henkilo"))
            .addHeader("Content-Type", CONTENT_TYPE)
            .setEntity(new OphHttpEntity.Builder()
                    .content(content)
                    .contentType(ContentType.APPLICATION_JSON)
                    .build())
            .build();
    ophHttpClient.execute(request)
            .handleErrorStatus(HttpServletResponse.SC_BAD_REQUEST)
            .with(responseAsString -> {
                if (retry && this.updateIfValidationErrorCanBeHandled(responseAsString, updateDto)) {
                    this.updateHenkilo(updateDto, false);
                    return Optional.empty();
                }
                throw new RestClientException(responseAsString);
            })
            .expectedStatus(SC_OK);

#### Get with return object
    OphHttpRequest request = OphHttpRequest.Builder
            .get(urlConfiguration.url("oppijanumerorekisteri-service.henkilo.hetu", hetu))
            .build();
    Optional<HenkiloDto> = ophHttpClient.<HenkiloDto>execute(request, TypeToken.get(HenkiloDto.class).getType())
            .expectedStatus(SC_OK);
