import React from 'react'
import { Accordion, Button, Table } from 'react-bootstrap'
import { RefereeSeason } from './data'
import {DateTime} from 'luxon'

interface Props {
  season: RefereeSeason
}

const AccordionSeason: React.VFC<Props> = ({ season }) => {
  const [statistikk, setStatistikk] = React.useState(false)
  return (
    <Accordion.Item eventKey={season.year.toString()}>
      <Accordion.Header>
        <div className="col align-self-start">
          <strong>{season.year}</strong>
        </div>
        <div className="col align-self-center">
          <small className="text-muted">
            <div>{season.averages.yellow.toFixed(2)} gule kort per kamp</div>
            <div>
              {(season.averages.yellowToRed + season.averages.red).toFixed(2)} røde kort per kamp
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
                <th>Turnering</th>
                <th>Kamp</th>
                <th>Statistikk</th>
              </tr>
            </thead>
            <tbody>
              {season.matches.map((match) => (
                <tr>
                  <td>{DateTime.fromISO(match.tidspunkt).setLocale('no').toFormat('dd-LLL')}</td>
                  <td>{match.tournament}</td>
                  <td>
                    <a href={`https://www.fotball.no/fotballdata/kamp/?fiksId=${match.fiksId}`} target="_blank">
                    {match.home} - {match.away}
                    </a>
                  </td>
                  <td className="small">
                    Røde {match.cards.red}
                    <br />
                    Gult nr 2 {match.cards.yellowToRed}
                    <br />
                    Gult {match.cards.yellow}
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

export default AccordionSeason
