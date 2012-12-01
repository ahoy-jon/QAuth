package lauth.model


trait Model {
  type User

  type Challenge
}


trait CompleteModel extends Model{

  type QRCode

  type QRURL

  type Challenge

  type Response

  type Site

  type UserInfo

  type Device

  type AuthServer

  type AuthRequest

  type QRReader = QRCode => QRURL

  type QRURLParser = QRURL => Challenge

  type ChallengeToSite = Challenge => Site

  type ResponseToChallenge = (Challenge, User, Time) => Response

  type Time
}


trait ChallengeService {
  self : Model =>

  def startAuth(challenge:Challenge)

  def registerUserForChallenge(challenge:Challenge, user:User)

  def userForChallenge(challenge:Challenge): Option[User]
}

trait InMemoModel extends Model {
  type User =  InMemoModel.User
  type Challenge = String
}

object InMemoModel {
  case class User(name:String,email:String)

}

object InProgress


trait InMemoryServiceImpl extends ChallengeService{
  self : Model =>

  private lazy val mmap = new collection.mutable.HashMap[Challenge,Either[InProgress.type ,User]]()

  def startAuth(challenge:Challenge) {}

  def registerUserForChallenge(challenge:Challenge, user:User) {
    mmap.put(challenge,Right(user))
  }

  def registerChallengeInProgress(challenge:Challenge) {
    mmap.put(challenge,Left(InProgress))
  }

  def userForChallenge(challenge:Challenge): Option[User] = {
    mmap.get(challenge).flatMap(_.right.toOption)
  }

  def inProgressForChallenge(challenge:Challenge):Boolean  = {
    mmap.get(challenge) match {
      case Some(Left(_)) => true
      case _ => false
    }
  }

}