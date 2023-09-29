import React, {useCallback, useState} from 'react'
import { Button, Col, Container, Form, Row, Spinner, Accordion, Alert } from 'react-bootstrap'
import AccordionSeason from './AccordionSeason'
import {IndexedReferee, RefereeStats} from './data'
import {AsyncTypeahead, Typeahead} from 'react-bootstrap-typeahead'
import {useSearchParams} from 'react-router-dom'


const App: React.VFC = () => {
  const [fetching, setFetching] = React.useState(false)
  const [state, setState] = React.useState<{loading:boolean, items:IndexedReferee[]}>({loading:false, items:[]})

  const [searchParams, setSearchParams] = useSearchParams({})

  const dommer = React.useMemo(() =>
      searchParams.get('fiksId') || ''
  , [searchParams.get('fiksId')])

  const setDommer = (fiksId:string) => setSearchParams({fiksId})


    const fetchDommer = async () => {try {
        setErrorMessage(undefined)
        setFetching(true)
        const response = await fetch(`/referee/${dommer}`)
        if (response.ok) {
          const stats: RefereeStats = await response.json()
          setRefereeStats(stats)
        } else {
          if(response.status == 504){
            setErrorMessage("Fikk ikke kontakt med fotball.no. Vi restarter, prøv igjen om 1 minutt")
          } else {
            console.log('oh nos', response)
            const error = await response.json()
            setErrorMessage(error.message || 'Ukjent feil')
          }
        }
      } finally {
        setFetching(false)
      }}

  React.useEffect( () => {
    console.log("useEffect", dommer)
    if(dommer) {
      fetchDommer()
    }
  }, [dommer])

  const [refereeStats, setRefereeStats] = React.useState<RefereeStats>()
  const [errorMessage, setErrorMessage] = React.useState<String>()

  const [activeIndex, setActiveIndex] = useState(-1);

  const onKeyDown = useCallback(
      (e) => {
        // Check whether the 'enter' key was pressed, and also make sure that
        // no menu items are highlighted.
        console.log("Key pressed", e.keyCode)
        if (e.keyCode === 13 && activeIndex === -1) {
          console.log("Enter!!", dommer)
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

  // const fiksId = React.useMemo(() => {
  //   if (dommer) {
  //     const parsed = Number.parseInt(dommer)
  //     if (parsed) {
  //       return parsed
  //     } else {
  //       return getFiksIdFromUrl(dommer)
  //     }
  //   } else {
  //     return null
  //   }
  // }, [dommer])



  return (
    <Container fluid>
      <h1><a href="/" style={{textDecoration: 'none', color: 'black'}}>Kortglad</a></h1>
      <p>Sjekk kortstatistikk</p>
      <p><small>Viser statistikk minimum 12 måneder tilbake i tid</small></p>
      <p><small>(Futsal er ikke med)</small></p>
      <p>
        <Form>
          <Row>
            <Col>
            </Col>
          </Row>
          <Row>
            <Col xs={7}>
              {errorMessage && (
                  <Alert variant="danger" onClose={() => setErrorMessage(undefined)} dismissible>
                    <p>{errorMessage}</p>
                  </Alert>
              )}
            </Col>
          </Row>
          <Row>
            <Col xs={7}>
              <AsyncTypeahead
                  placeholder="Søk etter dommer eller lim inn adresse til dommerdagbok fra fotball.no"
                  isLoading={state.loading}
                  onKeyDown={onKeyDown}
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
                      console.log("onChange", selected[0].fiksId)
                      setDommer(selected[0].fiksId.toString())
                    }else{
                      console.log("Ikke dommer" +selected)

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
              <Button variant="primary" onClick={fetchDommer} disabled={!dommer}>
                {fetching ? <Spinner as="span" animation="border" size="sm" /> && 'Henter statistikk' : 'Hent statistikk'}
              </Button>
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
