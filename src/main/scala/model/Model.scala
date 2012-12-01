package lauth.model


trait Model {

  type QRCode

  type QRURL

  type Challenge

  type Response

  type Site

  type UserInfo

  type User

  type Device

  type AuthServer

  type AuthRequest

  type QRReader = QRCode => QRURL

  type QRURLParser = QRURL => Challenge

  type ChallengeToSite = Challenge => Site

  type ResponseToChallenge = (Challenge, User, Time) => Response

  type Time


}
