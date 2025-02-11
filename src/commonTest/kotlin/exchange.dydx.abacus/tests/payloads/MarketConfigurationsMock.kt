package exchange.stream.abacus.tests.payloads

internal class MarketConfigurationsMock {
    internal val configurations = """
   {
     "1INCH-USD": {
       "name": "1inch",
       "tags": ["Defi"],
       "websiteLink": "https://1inch.io/",
       "whitepaperLink": "https://github.com/1inch",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/1inch/"
     },
     "AAVE-USD": {
       "name": "Aave",
       "tags": ["Defi"],
       "websiteLink": "https://aave.com/",
       "whitepaperLink": "https://github.com/aave/protocol-v2/blob/master/aave-v2-whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/aave/"
     },
     "ADA-USD": {
       "name": "Cardano",
       "tags": ["Layer 1"],
       "websiteLink": "https://cardano.org/",
       "whitepaperLink": "https://why.cardano.org/en/introduction/motivation/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/cardano/"
     },
     "AEVO-USD": {
       "name": "Aevo",
       "tags": ["Defi"],
       "websiteLink": "https://www.aevo.xyz/",
       "whitepaperLink": "https://docs.aevo.xyz/aevo-exchange/introduction",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/aevo/"
     },
     "AGIX-USD": {
       "name": "SingularityNET",
       "tags": ["AI"],
       "websiteLink": "https://public.singularitynet.io/whitepaper.pdf", 
       "whitepaperLink": "https://public.singularitynet.io/whitepaper.pdf", 
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/singularitynet/"
     },
     "ALGO-USD": {
       "name": "Algorand",
       "tags": ["Layer 1"],
       "websiteLink": "https://algorand.com/",
       "whitepaperLink": "https://algorand.com/technology/white-papers",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/algorand/"
     },
     "APE-USD": {
       "name": "ApeCoin",
       "tags": [],
       "websiteLink": "https://apecoin.com/",
       "whitepaperLink": "https://apecoin.com/about",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/apecoin-ape/"
     },
     "API3-USD": {
       "name": "API3",
       "tags": [],
       "websiteLink": "https://api3.org/",
       "whitepaperLink": "https://drive.google.com/file/d/1JMVwk9pkGF7hvjkuu6ABA0-FrhRTzAwF/view",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/api3/"
     },
     "APT-USD": {
       "name": "Aptos",
       "tags": ["Layer 1"],
       "websiteLink": "https://aptoslabs.com/",
       "whitepaperLink": "https://aptos.dev/aptos-white-paper/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/aptos/"
     },
     "ARB-USD": {
       "name": "Arbitrum",
       "tags": ["Layer 2"],
       "websiteLink": "https://arbitrum.io/",
       "whitepaperLink": "https://github.com/OffchainLabs/nitro",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/arbitrum/"
     },
     "ARKM-USD": {
       "name": "Arkham",
       "tags": [],
       "websiteLink": "https://www.arkhamintelligence.com/",
       "whitepaperLink": "https://www.arkhamintelligence.com/whitepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/arkham/"
     },
     "ASTR-USD": {
       "name": "Astar",
       "tags": ["Layer 2"],
       "websiteLink": "https://astar.network/",
       "whitepaperLink": "https://docs.astar.network/docs/getting-started",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/astar/"
     },
     "ATOM-USD": {
       "name": "Cosmos",
       "tags": ["Layer 1"],
       "websiteLink": "https://cosmos.network/",
       "whitepaperLink": "https://v1.cosmos.network/resources/whitepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/cosmos/"
     },
     "AVAX-USD": {
       "name": "Avalanche",
       "tags": ["Layer 1"],
       "websiteLink": "https://www.avalabs.org/",
       "whitepaperLink": "https://assets.website-files.com/5d80307810123f5ffbb34d6e/6008d7bbf8b10d1eb01e7e16_Avalanche%20Platform%20Whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/avalanche/"
     },
     "AXL-USD": {
       "name": "Axelar",
       "tags": [],
       "websiteLink": "https://axelar.network/",
       "whitepaperLink": "https://axelar.network/axelar_whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/axelar/"
     },
     "BCH-USD": {
       "name": "Bitcoin Cash",
       "tags": ["Layer 1"],
       "websiteLink": "https://bitcoincash.org/",
       "whitepaperLink": "https://bitcoincash.org/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/bitcoin-cash/"
     },
     "BOME-USD": {
       "name": "BOOK OF MEME",
       "tags": ["Meme"],
       "websiteLink": "https://llwapirxnupqu7xw2fspfidormcfar7ek2yp65nu7k5opjwhdywq.arweave.net/WuwHojdtHwp-9tFk8qBuiwRQR-RWsP91tPq656bHHi0",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/book-of-meme/"
     },
     "BONK-USD": {
       "name": "BONK COIN",
       "tags": ["Meme"],
       "websiteLink": "https://bonkcoin.com/",
       "whitepaperLink": "https://bonkcoin.com/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/bonk1/"
     },
     "BLUR-USD": {
       "name": "Blur",
       "tags": ["NFT"],
       "websiteLink": "https://blur.io/",
       "whitepaperLink": "https://docs.blur.foundation/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/blur-token/"
     },
     "BNB-USD":{
       "name": "BNB",
       "tags": ["Layer 1"],
       "websiteLink": "https://www.bnbchain.org/en",
       "whitepaperLink": "https://www.exodus.com/assets/docs/binance-coin-whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/bnb/"

     },
     "CHZ-USD": {
       "name": "Chiliz",
       "tags": ["Layer 1"],
       "websiteLink": "https://www.chiliz.com/", 
       "whitepaperLink": "https://www.chiliz.com/docs/litepaper-v1.1-20230703.pdf", 
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/chiliz/"
     },
     "CELO-USD": {
       "name": "Celo",
       "tags": [],
       "websiteLink": "https://celo.org",
       "whitepaperLink": "https://docs.celo.org",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/celo/"
     },
     "BTC-USD": {
       "name": "Bitcoin",
       "tags": ["Layer 1"],
       "websiteLink": "https://bitcoin.org/",
       "whitepaperLink": "https://bitcoin.org/bitcoin.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/bitcoin/"
     },
     "COMP-USD": {
       "name": "Compound",
       "tags": ["Defi"],
       "websiteLink": "https://compound.finance/",
       "whitepaperLink": "https://compound.finance/documents/Compound.Whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/compound/"
     },
     "CRV-USD": {
       "name": "Curve",
       "tags": ["Governance"],
       "websiteLink": "https://curve.fi/",
       "whitepaperLink": "https://curve.fi/whitepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/curve-dao-token/"
     },
     "DOGE-USD": {
       "name": "Dogecoin",
       "tags": ["Layer 1", "Meme"],
       "websiteLink": "https://dogecoin.com/",
       "whitepaperLink": "https://github.com/dogecoin/dogecoin",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/dogecoin/"
     },
     "DOT-USD": {
       "name": "Polkadot",
       "tags": ["Layer 1"],
       "websiteLink": "https://polkadot.network/",
       "whitepaperLink": "https://polkadot.network/PolkaDotPaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/polkadot-new/"
     },
     "DYDX-USD": {
       "name": "dYdX",
       "tags": ["Layer 1", "Defi"],
       "websiteLink": "https://dydx.exchange/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/dydx-chain/"
     },
     "DYM-USD": {
       "name": "Dymension",
       "tags": [],
       "websiteLink": "https://dymension.xyz/",
       "whitepaperLink": "https://docs.dymension.xyz/dymension-litepaper/dymension-litepaper-index",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/dymension/"
     },
     "ENJ-USD": {
       "name": "Enjin",
       "tags": [],
       "websiteLink": "https://enjin.io/",
       "whitepaperLink": "https://cdn.enjin.io/downloads/whitepapers/enjin-coin/en.pdf/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/enjin-coin/"
     },
     "ENS-USD": {
       "name": "Ethereum Name Service",
       "tags": [],
       "websiteLink": "https://coinmarketcap.com/currencies/ethereum-name-service/", 
       "whitepaperLink": "https://docs.ens.domains/", 
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/ethereum-name-service/"
     },
     "EOS-USD": {
       "name": "EOS",
       "tags": ["Layer 1"],
       "websiteLink": "https://eos.io/",
       "whitepaperLink": "https://github.com/EOSIO/Documentation/blob/master/TechnicalWhitePaper.md",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/eos/"
     },
     "ETC-USD": {
       "name": "Ethereum Classic",
       "tags": ["Layer 1"],
       "websiteLink": "https://ethereumclassic.org/",
       "whitepaperLink": "https://ethereumclassic.org/why-classic",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/ethereum-classic/"
     },
     "ETH-USD": {
       "name": "Ethereum",
       "tags": ["Layer 1"],
       "websiteLink": "https://ethereum.org/",
       "whitepaperLink": "https://ethereum.org/whitepaper/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/ethereum/",
       "displayStepSize": "0.001",
       "displayTickSize": "0.1"
     },
     "ETHFI-USD": {
       "name": "ether.fi",
       "tags": [],
       "websiteLink": "https://www.ether.fi/",
       "whitepaperLink": "https://etherfi.gitbook.io/etherfi/ether.fi-whitepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/ether-fi-ethfi/"
     },
     "FET-USD": {
       "name": "Fetch.ai",
       "tags": ["AI"],
       "websiteLink": "https://fetch.ai/",
       "whitepaperLink": "https://www.dropbox.com/s/gxptsecwdl3jjtn/David%20Minarsch%20-%202021-04-26%2010.34.17%20-%20paper_21_finalversion.pdf?e=1&dl=0",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/fetch/"
     },
     "FIL-USD": {
       "name": "Filecoin",
       "tags": ["Layer 1"],
       "websiteLink": "https://filecoin.io/",
       "whitepaperLink": "https://filecoin.io/filecoin.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/filecoin/"
     },
     "FLR-USD": {
       "name": "Flare",
       "tags": ["Layer 1"],
       "websiteLink": "https://flare.network/",
       "whitepaperLink": "https://docs.flare.network/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/flare/"
     },
     "FTM-USD": {
       "name": "Fantom",
       "tags": [],
       "websiteLink": "https://fantom.foundation/",
       "whitepaperLink": "https://fantom.foundation/_next/static/media/wp_fantom_v1.6.39329cdc5d0ee59684cbc6f228516383.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/fantom/"
     },
     "GALA-USD": {
       "name": "Gala",
       "tags": ["Gaming", "Layer 1"],
       "websiteLink": "https://gala.com/",
       "whitepaperLink": "https://galahackathon.com/v1.0.0/pdf/sdk-documentation.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/gala/"
     },
     "GMT-USD": {
       "name": "GMT",
       "tags": ["Gaming"],
       "websiteLink": "https://stepn.com/", 
       "whitepaperLink/": "https://whitepaper.stepn.com/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/green-metaverse-token/"
     },
     "GRT-USD": {
       "name": "The Graph",
       "tags": [],
       "websiteLink": "https://thegraph.com/", 
       "whitepaperLink/": "https://github.com/graphprotocol/research/blob/master/papers/whitepaper/the-graph-whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/the-graph/"
     },
     "HNT-USD": {
       "name": "Helium",
       "tags": ["Layer 1"],
       "websiteLink": "https://www.helium.com",
       "whitepaperLink": "http://whitepaper.helium.com",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/helium/"
     },
     "HBAR-USD": {
       "name": "Hedera",
       "tags": [],
       "websiteLink": "https://hedera.com/", 
       "whitepaperLink/": "https://files.hedera.com/hh_whitepaper_v2.2-20230918.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/hedera/"
     },
     "ICP-USD": {
       "name": "Internet Computer",
       "tags": ["Layer 1"],
       "websiteLink": "https://dfinity.org",
       "whitepaperLink": "https://dfinity.org/whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/internet-computer/"
     },
     "IMX-USD": {
       "name": "Immutable X",
       "tags": ["Gaming", "Layer 2", "NFT"],
       "websiteLink": "https://www.immutable.com/",
       "whitepaperLink": "https://assets.website-files.com/646557ee455c3e16e4a9bcb3/6499367de527dd82ab7475a3_Immutable%20Whitepaper%20Update%202023%20(3).pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/immutable-x/"
     },
     "INJ-USD": {
       "name": "Injective",
       "tags": ["Layer 1", "Defi"],
       "websiteLink": "https://injective.com/",
       "whitepaperLink": "https://docs.injective.network/intro/01_overview.html",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/injective/"
     },
     "JTO-USD": {
       "name": "Jito",
       "tags": ["Defi"],
       "websiteLink": "https://www.jito.network/",
       "whitepaperLink": "https://github.com/jito-foundation",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/jito/"
     },
     "JUP-USD": {
       "name": "Jupiter",
       "tags": ["Defi"],
       "websiteLink": "https://station.jup.ag/",
       "whitepaperLink": "https://station.jup.ag/blog/green-paper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/jupiter-ag/"
     },
     "KAVA-USD": {
       "name": "Kava",
       "tags": ["Layer 1"],
       "websiteLink": "https://www.kava.io/",
       "whitepaperLink": "https://docsend.com/view/gwbwpc3",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/kava/"
     },
     "LDO-USD": {
       "name": "Lido DAO",
       "tags": ["Defi"],
       "websiteLink": "https://lido.fi/",
       "whitepaperLink": "https://lido.fi/static/Lido:Ethereum-Liquid-Staking.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/lido-dao/"
     },
     "LINK-USD": {
       "name": "Chainlink",
       "tags": ["Defi"],
       "websiteLink": "https://chain.link/",
       "whitepaperLink": "https://link.smartcontract.com/whitepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/chainlink/"
     },
     "LTC-USD": {
       "name": "Litecoin",
       "tags": ["Layer 1"],
       "websiteLink": "https://litecoin.org/",
       "whitepaperLink": "https://litecoin.info/index.php/Main_Page",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/litecoin/"
     },
     "MAGIC-USD": {
       "name": "MAGIC",
       "tags": ["NFT"],
       "websiteLink": "https://treasure.lol/",
       "whitepaperLink": "https://files.treasure.lol/Treasure+Infinity+Chains+-+Litepaper+v1.0.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/magic-token/"
     },
     "MANA-USD": {
       "name": "Decentraland",
       "tags": ["AR/VR"],
       "websiteLink": "https://decentraland.org/",
       "whitepaperLink": "https://decentraland.org/whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/decentraland/"
     },
     "MASK-USD": {
       "name": "Mask Network",
       "tags": [],
       "websiteLink": "https://mask.io/",
       "whitepaperLink": "https://masknetwork.medium.com/introducing-mask-network-maskbook-the-future-of-the-internet-5a973d874edd",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/mask-network/"
     },
     "MATIC-USD": {
       "name": "Polygon",
       "tags": ["Layer 2"],
       "websiteLink": "https://polygon.technology/",
       "whitepaperLink": "https://polygon.technology/lightpaper-polygon.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/polygon/"
     },
     "MEME-USD": {
       "name": "Memecoin",
       "tags": ["Meme"],
       "websiteLink": "https://www.memecoin.org/",
       "whitepaperLink": "https://www.memecoin.org/whitepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/meme/"
     },
     "MINA-USD": {
       "name": "Mina",
       "tags": ["Layer 1"],
       "websiteLink": "https://minaprotocol.com/",
       "whitepaperLink": "https://docs.minaprotocol.com/assets/economicsWhitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/mina/"
     },
     "MKR-USD": {
       "name": "Maker",
       "tags": ["Governance"],
       "websiteLink": "https://makerdao.com",
       "whitepaperLink": "https://makerdao.com/whitepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/maker/"
     },
     "NEAR-USD": {
       "name": "NEAR Protocol",
       "tags": ["Layer 1"],
       "websiteLink": "https://near.org",
       "whitepaperLink": "https://near.org/papers/the-official-near-white-paper/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/near-protocol/"
     },
     "OCEAN-USD": {
       "name": "Ocean Protocol",
       "tags": ["AI"],
       "websiteLink": "https://oceanprotocol.com/",
       "whitepaperLink": "https://oceanprotocol.com/tech-whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/ocean-protocol/"
     },
     "ORDI-USD": {
       "name": "Ordinals",
       "tags": ["NFT"],
       "websiteLink": "https://ordinals.com/",
       "whitepaperLink": "https://rodarmor.com/blog/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/ordi/"
     },
     "OP-USD": {
       "name": "Optimism",
       "tags": ["Layer 2"],
       "websiteLink": "https://www.optimism.io/",
       "whitepaperLink": "https://github.com/ethereum-optimism",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/optimism-ethereum/"
     },
     "PEPE-USD": {
       "name": "Pepe",
       "tags": ["Meme"],
       "websiteLink": "https://www.pepe.vip/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/pepe/"
     },
     "PORTAL-USD": {
       "name": "PORTAL",
       "tags": ["Gaming"],
       "websiteLink": "https://www.portalgaming.com/",
       "whitepaperLink": "https://portalxyz.nyc3.cdn.digitaloceanspaces.com/Portal_Whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/portal-gaming/"
     },
     "PYTH-USD": {
       "name": "Pyth Network",
       "tags": [],
       "websiteLink": "https://pyth.network/",
       "whitepaperLink": "https://pyth.network/whitepaper_v2.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/pyth-network/"
     },
     "RNDR-USD": {
       "name": "Render Token",
       "tags": ["AI"],
       "websiteLink": "https://rendernetwork.com/",
       "whitepaperLink": "https://renderfoundation.com/whitepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/render/"
     },
     "RUNE-USD": {
       "name": "THORChain",
       "tags": ["Layer 1"],
       "websiteLink": "https://thorchain.org",
       "whitepaperLink": "https://whitepaper.io/document/709/thorchain-whitepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/thorchain/"
     },
     "SAND-USD": {
       "name": "The Sandbox",
       "tags": ["Gaming"],
       "websiteLink": "https://www.sandbox.game/en/",
       "whitepaperLink": "https://installers.sandbox.game/The_Sandbox_Whitepaper_2020.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/the-sandbox/"
     },
     "SEI-USD": {
       "name": "Sei",
       "tags": ["Layer 1", "Defi"],
       "websiteLink": "https://www.sei.io/",
       "whitepaperLink": "https://github.com/sei-protocol/sei-chain/blob/3c9576fee3494ce039df684624f918dd8066ba3f/whitepaper/Sei_Whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/sei/"
     },
     "SHIB-USD": {
       "name": "Shiba Inu",
       "tags": ["Meme"],
       "websiteLink": "https://shibatoken.com/",
       "whitepaperLink": "https://docs.shibatoken.com/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/shiba-inu/"
     },
     "SNX-USD": {
       "name": "Synthetix",
       "tags": ["Defi"],
       "websiteLink": "https://synthetix.io/",
       "whitepaperLink": "https://docs.synthetix.io/litepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/synthetix-network-token/"
     },
     "SOL-USD": {
       "name": "Solana",
       "tags": ["Layer 1"],
       "websiteLink": "https://solana.com/",
       "whitepaperLink": "https://solana.com/solana-whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/solana/"
     },
     "STRK-USD": {
       "name": "Starknet",
       "tags": ["Layer 2"],
       "websiteLink": "https://www.starknet.io/en",
       "whitepaperLink": "https://docs.starknet.io/documentation/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/starknet-token/"
     },
     "STX-USD": {
       "name": "Stacks",
       "tags": ["Layer 2"],
       "websiteLink": "https://www.stacks.co/",
       "whitepaperLink": "https://gaia.blockstack.org/hub/1AxyPunHHAHiEffXWESKfbvmBpGQv138Fp/stacks.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/stacks/"
     },
     "SUI-USD": {
       "name": "Sui",
       "tags": ["Layer 1"],
       "websiteLink": "https://sui.io/",
       "whitepaperLink": "https://github.com/MystenLabs",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/sui/"
     },
     "SUSHI-USD": {
       "name": "Sushi",
       "tags": ["Defi"],
       "websiteLink": "https://sushi.com/",
       "whitepaperLink": "https://docs.sushi.com/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/sushiswap/"
     },
     "TIA-USD": {
       "name": "Celestia",
       "tags": ["Layer 1"],
       "websiteLink": "https://celestia.org/",
       "whitepaperLink": "https://arxiv.org/pdf/1905.09274.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/celestia/"
     },
     "TON-USD": {
       "name": "Toncoin",
       "tags": ["Layer 1"],
       "websiteLink": "https://ton.org/",
       "whitepaperLink": "https://ton.org/whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/toncoin/"
     },
     "TRX-USD": {
       "name": "TRON",
       "tags": ["Defi"],
       "websiteLink": "https://tron.network/",
       "whitepaperLink": "https://tron.network/static/doc/white_paper_v_2_0.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/tron/"
     },
     "UMA-USD": {
       "name": "UMA",
       "tags": ["Defi"],
       "websiteLink": "https://umaproject.org/",
       "whitepaperLink": "https://github.com/UMAprotocol/whitepaper",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/uma/"
     },
     "UNI-USD": {
       "name": "Uniswap",
       "tags": ["Defi"],
       "websiteLink": "https://uniswap.org/",
       "whitepaperLink": "https://uniswap.org/whitepaper-v3.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/uniswap/"
     },
     "WIF-USD": {
       "name": "dogwifhat",
       "tags": ["Meme"],
       "websiteLink": "https://dogwifcoin.org/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/dogwifhat/"
     },
     "WLD-USD": {
       "name": "Worldcoin",
       "tags": [],
       "websiteLink": "https://worldcoin.org/",
       "whitepaperLink": "https://whitepaper.worldcoin.org/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/worldcoin-org/"
     }, 
     "WOO-USD": {
       "name": "WOO Network",
       "tags": ["Defi"],
       "websiteLink": "https://woo.org/",
       "whitepaperLink": "https://woo.org/Litepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/wootrade/"
     },
     "XLM-USD": {
       "name": "Stellar",
       "tags": ["Layer 1"],
       "websiteLink": "https://www.stellar.org/",
       "whitepaperLink": "https://www.stellar.org/papers/stellar-consensus-protocol",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/stellar/"
     },
     "XMR-USD": {
       "name": "Monero",
       "tags": ["Layer 1"],
       "websiteLink": "https://www.getmonero.org/",
       "whitepaperLink": "https://www.getmonero.org/resources/research-lab/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/monero/"
     },
     "XRP-USD": {
       "name": "Ripple",
       "tags": ["Layer 1"],
       "websiteLink": "https://ripple.com/currency/",
       "whitepaperLink": "https://github.com/ripple",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/xrp/"
     },
     "XTZ-USD": {
       "name": "Tezos",
       "tags": ["Layer 1"],
       "websiteLink": "https://tezos.com",
       "whitepaperLink": "https://tezos.com/whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/tezos/"
     },
     "YFI-USD": {
       "name": "Yearn",
       "tags": ["Defi"],
       "websiteLink": "https://yearn.finance/",
       "whitepaperLink": "https://docs.yearn.finance/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/yearn-finance/"
     },
     "ZEC-USD": {
       "name": "Zcash",
       "tags": ["Layer 1"],
       "websiteLink": "https://z.cash/",
       "whitepaperLink": "https://z.cash/technology/",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/zcash/"
     },
     "ZETA-USD": {
       "name": "ZetaChain",
       "tags": ["Layer 1"],
       "websiteLink": "https://www.zetachain.com/",
       "whitepaperLink": "https://www.zetachain.com/whitepaper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/zetachain/"
     },
     "ZRX-USD": {
       "name": "0x",
       "tags": ["Defi"],
       "websiteLink": "https://0x.org/",
       "whitepaperLink": "https://0x.org/pdfs/0x_white_paper.pdf",
       "coinMarketCapsLink": "https://coinmarketcap.com/currencies/0x/"
     }
   }

    """.trimIndent()
}
