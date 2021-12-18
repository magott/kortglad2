export interface CardStat {
  yellow: number
  yellowToRed: number
  red: number
}

export interface MatchStat {
  fiksId: string
  tidspunkt: string
  home: string
  away: string
  cards: CardStat
}

export interface RefereeSeason {
  year: number
  matches: MatchStat[]
  totals: CardStat
  averages: CardStat
}

export interface RefereeStats {
  refereeName: string
  seasons: RefereeSeason[]
}
