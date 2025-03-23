const TelegramBot = require('node-telegram-bot-api');
const dotenv = require('dotenv');

// Load environment variables from .env file
dotenv.config();

// Get bot token from environment variable
const token = process.env.TELEGRAM_BOT_TOKEN;

if (!token) {
  console.error('TELEGRAM_BOT_TOKEN environment variable is not set');
  process.exit(1);
}

// Create a bot instance
const bot = new TelegramBot(token, { polling: true });

console.log('Starting MFA OTP Telegram bot...');

// Respond to /start command
bot.onText(/\/start/, (msg) => {
  const chatId = msg.chat.id;
  bot.sendMessage(
    chatId,
    `Welcome to the MFA OTP bot!\n\nYour Chat ID is: ${chatId}\n\nUse this ID to configure Telegram authentication in the MFA system.`
  );
});

// Respond to /id command
bot.onText(/\/id/, (msg) => {
  const chatId = msg.chat.id;
  bot.sendMessage(
    chatId,
    `Your Chat ID is: ${chatId}`
  );
});

// Respond to any message with the chat ID
bot.on('message', (msg) => {
  // Skip handling commands (already handled above)
  if (msg.text && (msg.text.startsWith('/start') || msg.text.startsWith('/id'))) {
    return;
  }
  
  const chatId = msg.chat.id;
  bot.sendMessage(
    chatId,
    `Your Chat ID is: ${chatId}\n\nUse this ID to configure Telegram authentication in the MFA system.`
  );
});

// Handle errors
bot.on('polling_error', (error) => {
  console.error('Telegram bot polling error:', error);
});

console.log('Telegram bot is running...');