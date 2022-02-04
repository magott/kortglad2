import React, {useCallback, useState} from 'react'
import { Button, Col, Container, Form, Row, Spinner, Accordion, Alert } from 'react-bootstrap'
import AccordionSeason from './AccordionSeason'
import {IndexedReferee, RefereeStats} from './data'
import {AsyncTypeahead, Typeahead} from 'react-bootstrap-typeahead'


const App: React.VFC = () => {
  const [fetching, setFetching] = React.useState(false)
  const [state, setState] = React.useState<{loading:boolean, items:IndexedReferee[]}>({loading:false, items:[]})

  const [dommer, setDommer] = React.useState(
    () => new URLSearchParams(new URL(window.location.href).search).get('fiksId') || ''
  )
  const [refereeStats, setRefereeStats] = React.useState<RefereeStats>()
  const [errorMessage, setErrorMessage] = React.useState<String>()

  const [activeIndex, setActiveIndex] = useState(-1);

  const onKeyDown = useCallback(
      (e) => {
        // Check whether the 'enter' key was pressed, and also make sure that
        // no menu items are highlighted.
        console.log("Key pressed", e)
        if (e.keyCode === 13 && activeIndex === -1) {
          console.log("Enter!!")
          if(fiksId != null && fiksId != ""){
            hentStatistikk()
          }
        }
      },
      [activeIndex, dommer]
  );

  function getFiksIdFromUrl(candidate:string){
    console.log("Getting fiksid from url "+candidate)
    try {
      const url = new URL(candidate)
      const search = new URLSearchParams(url.search)
      const fiks = search.get('fiksId')
      return fiks
    } catch (error) {
      return null
    }
  }

  const fiksId = React.useMemo(() => {
    if (dommer) {
      const parsed = Number.parseInt(dommer)
      if (parsed) {
        return parsed
      } else {
        return getFiksIdFromUrl(dommer)
      }
    } else {
      return null
    }
  }, [dommer])

  const hentStatistikk = async () => {
    console.log("Henter statistikk for "+fiksId)
    if (fiksId !== null) {
      history.pushState({}, '', `?fiksId=${fiksId}`)
      try {
        setErrorMessage(undefined)
        setFetching(true)
        const response = await fetch(`/referee/${fiksId}`)
        if (response.ok) {
          const stats: RefereeStats = await response.json()
          setRefereeStats(stats)
        } else {
          console.log('oh nos', response)
          const error = await response.json()
          setErrorMessage(error.message || 'Ukjent feil')
        }
      } finally {
        setFetching(false)
      }
    } else {
      history.pushState({}, '', '')
    }
  }

  return (
    <Container fluid>
      <h1>Kortglad</h1>
      <p>Sjekk kortstatistikk fra og med 2021-sesongen</p>
      <p>(Futsal er ikke med!)</p>
      <p>
        <Form>
          <Row>
            <Col>
            </Col>
          </Row>
          <Row>
            <Col xs={7}>
              <AsyncTypeahead
                  placeholder="Søk etter dommer eller lim inn adresse til dommerdagbok fra fotball.no"
                  isLoading={state.loading}
                  labelKey={(option) => option.name}
                  onSearch={(query) => {
                    setState({...state, loading: true});
                    fetch(`search/referee?q=${query}`)
                        .then(resp => resp.json())
                        .then(items => setState({
                          loading: false,
                          items,
                        }));
                  }}
                  options={state.items}
                  onChange={(selected) => {
                    if(selected.length == 1) {
                      console.log(selected[0].fiksId)
                      setDommer(selected[0].fiksId.toString())
                    }else{
                      console.log("Ikke dommer" +selected)
                      setDommer('')
                    }
                  }}
                  onInputChange={(input, e: Event) => {
                    const fiksIdFromUrl = getFiksIdFromUrl(input)
                    if (fiksIdFromUrl != null) {
                      console.log("Setter dommer med fiksId "+fiksIdFromUrl)
                      setDommer(fiksIdFromUrl)
                    } else {
                      console.log("Ikke dommerurl" + input)
                    }
                  }
                  }
              >

              </AsyncTypeahead>
            </Col>
            <Col xs={3}>
              <Button variant="primary" onClick={() => hentStatistikk()} disabled={fiksId == null}>
                {fetching && <Spinner as="span" animation="border" size="sm" />}
                Hent statistikk
              </Button>
            </Col>
          </Row>
          <Row>
            <Col>
              {errorMessage && (
                <Alert variant="danger" onClose={() => setErrorMessage(undefined)} dismissible>
                  <Alert.Heading>{errorMessage}</Alert.Heading>
                  <p>Har du skrevet riktig adresse til dommerdagbok fra fotball.no?</p>
                </Alert>
              )}
            </Col>
          </Row>
        </Form>
      </p>
      {refereeStats && (
        <p>
          <h4>{refereeStats.refereeName}</h4>
          {refereeStats.seasons && refereeStats.seasons.length > 0 && (
            <Accordion>
              {refereeStats.seasons.map((season) => (
                <AccordionSeason season={season} key={season.year} />
              ))}
            </Accordion>
          )}
        </p>
      )}
    </Container>
  )
}

export default App
