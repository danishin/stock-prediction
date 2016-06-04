package common

import java.time.LocalDate

case class Article(id: Int, title: String, tags: Seq[String], preview: String, content: String, date: LocalDate)
