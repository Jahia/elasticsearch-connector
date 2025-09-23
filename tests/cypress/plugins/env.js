module.exports = (on, config) => {
    config.env.ELASTIC_PASSWORD_ENCODED = process.env.ELASTIC_PASSWORD_ENCODED
}
