const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendNotificationOnMessage = functions.firestore
  .document("chats/{chatId}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {

    const message = snapshot.data();

    const senderUid = message.sender;

    // 🔥 get all users
    const usersSnap = await admin.firestore().collection("users").get();

    let tokens = [];

    usersSnap.forEach(doc => {
      const data = doc.data();

      // ❗ sender ki notification pampakudadhu
      if (doc.id !== senderUid && data.fcmToken) {
        tokens.push(data.fcmToken);
      }
    });

    if (tokens.length === 0) return null;

    const payload = {
      notification: {
        title: "New Message 💬",
        body: message.original
      },
      data: {
        type: "chat"
      }
    };

    await admin.messaging().sendToDevice(tokens, payload);

    return null;
  });