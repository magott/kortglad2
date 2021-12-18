import React from 'react'
import { Button, Col, Container, Form, Row, Spinner, Accordion, Alert } from 'react-bootstrap'
import AccordionSeason from './AccordionSeason'
import { RefereeStats } from './data'

const App: React.VFC = () => {
  const [fetching, setFetching] = React.useState(false)
  const [dommer, setDommer] = React.useState(
    () => new URLSearchParams(new URL(window.location.href).search).get('fiksId') || ''
  )
  const [refereeStats, setRefereeStats] = React.useState<RefereeStats>()
  const [errorMessage, setErrorMessage] = React.useState<String>()

  const fiksId = React.useMemo(() => {
    if (dommer) {
      const parsed = Number.parseInt(dommer)
      if (parsed) {
        return parsed
      } else {
        try {
          const url = new URL(dommer)
          const search = new URLSearchParams(url.search)
          const fiks = search.get('fiksId')
          return fiks
        } catch (error) {
          return null
        }
      }
    } else {
      return null
    }
  }, [dommer])

  const hentStatistikk = async () => {
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
            <Col xs={7}>
              <Form.Control
                type="input"
                value={dommer}
                onChange={(e) => setDommer(e.target.value)}
                placeholder="https://www.fotball.no/fotballdata/person/dommeroppdrag/?fiksId=xxxx"
              />
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
