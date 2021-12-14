package kortglad

import bloque.jetty.*

@main def main = Jetty(8080, "./public") { App.run }
