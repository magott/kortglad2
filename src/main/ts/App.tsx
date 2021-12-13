import React from 'react'
import { Button, Col, Container, Form, Row, Spinner, Table, Collapse, Accordion } from 'react-bootstrap'

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
  season: bigint
  matches: MatchStat[]
  totals: CardStat
  averages: CardStat
}

interface RefereeStats {
  refereeName: string
  seasons: RefereeSeason[]
}

const App: React.VFC = () => {
  const [fetching, setFetching] = React.useState(false)
  const [dommer, setDommer] = React.useState('')
  const [refereeStats, setRefereeStats] = React.useState<RefereeStats>()
  const [statistikk, setStatistikk] = React.useState(false)

  const fiksId = React.useMemo(() => {
    if (dommer) {
      const parsed = Number.parseInt(dommer)
      if (parsed) {
        return parsed
      } else {
        try {
          const url = new URL(dommer)
          const search = new URLSearchParams(url.search)
          return search.get('fiksId')
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
      try {
        setFetching(true)
        const response = await fetch(`/referee/${fiksId}`)
        const stats: RefereeStats = await response.json()
        setRefereeStats(stats)
      } finally {
        setFetching(false)
      }
    }
  }

  return (
    <Container fluid>
      <h1>Kortglad</h1>
      <p>Sjekk kortstatistikk for dommer innev&aelig;rende sesong</p>
      <p>(Futsal er ikke med!)</p>
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
      </Form>
      {refereeStats && <div>
        <h4>{refereeStats.refereeName}</h4>
        {refereeStats.seasons && refereeStats.seasons.length > 0 &&
            <>
              <Accordion>
                {refereeStats.seasons.map((season) =>
                    <Accordion.Item eventKey="{season.season}">
                      <Accordion.Header>{season.season} - {season.averages.yellow.toFixed(2)} snitt gule - {(season.averages.yellowToRed + season.averages.red).toFixed(2)} snitt røde</Accordion.Header>
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
                            Vis statistikk per kamp ({season.matches.length})
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
                                      <br/>
                                      Gult nr 2 {match.cards.yellowToRed}
                                      <br/>
                                      Gult {match.cards.yellow}
                                    </td>
                                    <td>
                                      <a href={`https://www.fotball.no/fotballdata/kamp/?fiksId=${match.fiksId}`}>
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
                )
                }
              </Accordion>
            </>
        }
      </div>
      }
    </Container>
  )
}

export default App
