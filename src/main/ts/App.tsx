import React from 'react'
import {
  Button,
  Col,
  Container,
  Form,
  Row,
  Spinner,
  Table,
  Collapse,
  Accordion, Alert,
} from 'react-bootstrap'

interface CardStat {
  yellow: number
  yellowToRed: number
  red: number
}

interface MatchStat {
  fiksId: string
  tidspunkt: string
  home: string
  away: string
  cards: CardStat
}

interface RefereeSeason {
  season: number
  matches: MatchStat[]
  totals: CardStat
  averages: CardStat
}

interface RefereeStats {
  refereeName: string
  seasons: RefereeSeason[]
}

interface Error {
  message: string
}

const App: React.VFC = () => {
  const [fetching, setFetching] = React.useState(false)
  const [dommer, setDommer] = React.useState(
    () => new URLSearchParams(new URL(window.location.href).search).get('fiksId') || ''
  )
  const [refereeStats, setRefereeStats] = React.useState<RefereeStats>()
  const [statistikk, setStatistikk] = React.useState(false)
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
        if(response.ok) {
          const stats: RefereeStats = await response.json()
          setRefereeStats(stats)
        } else {
          console.log("oh nos", response)
          const error: Error = await response.json()
          setErrorMessage(error.message || "Ukjent feil")
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
              {errorMessage && <Alert variant="danger" onClose={() => setErrorMessage(undefined)} dismissible>
                <Alert.Heading>{errorMessage}</Alert.Heading>
                <p>
                  Har du skrevet riktig adresse til dommerdagbok fra fotball.no?
                </p>
              </Alert>}
            </Col>
          </Row>
        </Form>
      </p>
      {refereeStats && (
        <p>
          <h4>{refereeStats.refereeName}</h4>
          {refereeStats.seasons && refereeStats.seasons.length > 0 && (
            <>
              <Accordion>
                {refereeStats.seasons.map((season) => (
                  <Accordion.Item eventKey="{season.season}">
                    <Accordion.Header>
                      <div className="col align-self-start">
                        <strong>{season.season}</strong>
                      </div>
                      <div className="col align-self-center">
                        <small className="text-muted">
                          <div>{season.averages.yellow.toFixed(2)} gule kort per kamp</div>
                          <div>
                            {(season.averages.yellowToRed + season.averages.red).toFixed(2)} røde
                            kort per kamp
                          </div>
                        </small>
                      </div>
                    </Accordion.Header>
                    <Accordion.Body>
                      <Table>
                        <thead>
                          <tr>
                            <th></th>
                            <th>Snitt</th>
                            <th>Totalt</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td>Gult</td>
                            <td>{season.averages.yellow.toFixed(2)}</td>
                            <td>{season.totals.yellow}</td>
                          </tr>
                          <tr>
                            <td>Gult nr. 2</td>
                            <td>{season.averages.yellowToRed.toFixed(2)}</td>
                            <td>{season.totals.yellowToRed}</td>
                          </tr>
                          <tr>
                            <td>Rødt</td>
                            <td>{season.averages.red.toFixed(2)}</td>
                            <td>{season.totals.red}</td>
                          </tr>
                        </tbody>
                      </Table>
                      <h5>
                        <Button variant="primary" onClick={() => setStatistikk(!statistikk)}>
                          {!statistikk ? (
                            <>Vis statistikk per kamp ({season.matches.length})</>
                          ) : (
                            <>Skjul kamper</>
                          )}
                        </Button>
                      </h5>
                      {statistikk && (
                        <Table>
                          <thead>
                            <tr>
                              <th>Dato</th>
                              <th>Kamp</th>
                              <th>Statistikk</th>
                              <th>Link</th>
                            </tr>
                          </thead>
                          <tbody>
                            {season.matches.map((match) => (
                              <tr>
                                <td>{match.tidspunkt.replace('T', ' ')}</td>
                                <td>{match.home}</td>
                                <td className="small">
                                  Røde {match.cards.red}
                                  <br />
                                  Gult nr 2 {match.cards.yellowToRed}
                                  <br />
                                  Gult {match.cards.yellow}
                                </td>
                                <td>
                                  <a
                                    href={`https://www.fotball.no/fotballdata/kamp/?fiksId=${match.fiksId}`}
                                  >
                                    fotball.no
                                  </a>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </Table>
                      )}
                    </Accordion.Body>
                  </Accordion.Item>
                ))}
              </Accordion>
            </>
          )}
        </p>
      )}
    </Container>
  )
}

export default App
