package common

import javax.inject.Singleton

import reactivemongo.api.MongoConnection

@Singleton
class Config {
  private def getEnv(key: String): Option[String] = sys.env.get(key)

  // Use mongolab
  val MongoDBParsedURI = MongoConnection.parseURI(getEnv("MONGODB_URI").getOrElse("mongodb://localhost:27017")).get
  val MongoDBDatabaseName = "danishin_com"
}
