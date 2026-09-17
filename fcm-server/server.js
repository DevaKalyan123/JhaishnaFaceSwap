const express = require("express");
const cors = require("cors");
const admin = require("firebase-admin");

const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const app = express();

app.use(cors());
app.use(express.json());

app.post("/send-call", async (req, res) => {
  try {
    const { token, callerName, callId } = req.body;

    const message = {
      token: token,
      data: {
        callerName: callerName,
        callId: callId,
        openIncoming: "true"
      },
      android: {
        priority: "high"
      }
    };

    const response = await admin.messaging().send(message);

    res.status(200).json({
      success: true,
      response
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

app.listen(3000, () => {
  console.log("🔥 FCM Server running on port 3000");
});