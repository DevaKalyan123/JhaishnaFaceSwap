const express = require("express");
const admin = require("firebase-admin");

const app = express();
app.use(express.json());

const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

app.post("/send", async (req, res) => {
  const { token, title, body } = req.body;

  const message = {
    token: token,
    notification: {
      title: title,
      body: body,
    },
  };

  try {
    await admin.messaging().send(message);
    res.send("Notification sent");
  } catch (e) {
    res.status(500).send(e);
  }
});

app.listen(3000, () => {
  console.log("Server running on 3000");
});