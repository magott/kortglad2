import React from "react";
import { Button, Col, Container, Form, Row, Spinner, Table } from "react-bootstrap";

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

interface RefereeStats {
  matches: MatchStat[]
  refereeName: string
  totals: CardStat
  averages: CardStat
}

const App: React.VFC = () => {
  const [fetching, setFetching] = React.useState(false)
  const [dommer, setDommer] = React.useState("")
  const [refereeStats, setRefereeStats] = React.useState<RefereeStats>()

  const fiksId = React.useMemo(() => { 
    console.log('dommer', dommer)
    if(dommer){
      const url = new URL(dommer)
      const search = new URLSearchParams(url.search)      
      return search.get('fiksId')
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
      <p>(Futsal er ikke med)</p>
      <Form>
        <Row>
          <Col xs={7}>
            <Form.Control type="input" value={dommer} onChange={(e) => setDommer(e.target.value) } placeholder="https://www.fotball.no/fotballdata/person/dommeroppdrag/?fiksId=xxxx"  />
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
              <td>{refereeStats.averages.yellow.toFixed(2)}</td>
              <td>{refereeStats.totals.yellow}</td>
            </tr>
            <tr>
              <td>Gult nr. 2</td>
              <td>{refereeStats.averages.yellowToRed.toFixed(2)}</td>
              <td>{refereeStats.totals.yellowToRed}</td>
            </tr>
            <tr>
              <td>Rødt</td>
              <td>{refereeStats.averages.red.toFixed(2)}</td>
              <td>{refereeStats.totals.red}</td>
            </tr>
          </tbody>
        </Table>
      </div>
      }
    </Container>
  )
}

export default App