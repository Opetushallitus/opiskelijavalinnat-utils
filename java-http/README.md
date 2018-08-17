# Java-http module

## OphHttpClient

### Usage
Some examples

#### Put with an error handler and without return value
    OphHttpRequest request = OphHttpRequest.Builder
            .put(urlConfiguration.url("oppijanumerorekisteri-service.s2s.henkilo"))
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            .setEntity(new OphHttpEntity.Builder()
                    .content(content)
                    .contentType(ContentType.APPLICATION_JSON)
                    .build())
            .build();
    ophHttpClient.execute(request)
            .handleErrorStatus(HttpServletResponse.SC_BAD_REQUEST)
            .with(responseAsString -> {
                if (this.updateIfValidationErrorCanBeHandled(responseAsString, updateDto)) {
                    this.updateHenkilo(updateDto, false);
                    return Optional.empty();
                }
                throw new RestClientException(responseAsString);
            })
            .expectedStatus(SC_OK)
            .ignoreResponse();

#### Get with return object and jackson mapping
    OphHttpRequest request = OphHttpRequest.Builder
            .get(urlConfiguration.url("oppijanumerorekisteri-service.henkilo.hetu", hetu))
            .build();
    ophHttpClient.<HenkiloDto>execute(request)
            .expectedStatus(SC_OK).mapWith(text -> {
                try {
                    return this.objectMapper.readValue(text, HenkiloDto.class);
                } catch (IOException jpe) {
                    throw new RestClientException(jpe.getMessage());
                }
            });
