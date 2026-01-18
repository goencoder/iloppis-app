import Foundation

struct TransactionItem: Identifiable, Equatable {
    let id: String
    let sellerNumber: Int
    let price: Int
    var status: TransactionStatus

    init(
        id: String = UUID().uuidString,
        sellerNumber: Int,
        price: Int,
        status: TransactionStatus = .pending
    ) {
        self.id = id
        self.sellerNumber = sellerNumber
        self.price = price
        self.status = status
    }
}

enum TransactionStatus: String, Equatable {
    case pending
    case uploaded
    case failed
}

enum PaymentMethodType: String, Equatable {
    case cash
    case swish
}

struct CompletedPurchase: Equatable {
    let purchaseId: String
    let items: [TransactionItem]
    let total: Int
    let paymentMethod: PaymentMethodType
}

enum ActiveField: Equatable {
    case seller
    case price
}

struct CashierState: Equatable {
    let eventName: String

    var sellerNumber: String = ""
    var priceString: String = ""
    var activeField: ActiveField = .seller

    var transactions: [TransactionItem] = []
    var paidAmount: String = "0"

    var validSellers: Set<Int> = []

    var lastPurchase: CompletedPurchase? = nil

    var isLoading: Bool = false
    var isProcessingPayment: Bool = false
    var errorMessage: String? = nil
    var warningMessage: String? = nil

    var total: Int {
        transactions.reduce(0) { $0 + $1.price }
    }

    var change: Int {
        (Int(paidAmount) ?? 0) - total
    }

    var nextHundred: Int {
        guard total > 0 else { return 0 }
        return Int(ceil(Double(total) / 100.0) * 100.0)
    }
}

enum CashierAction {
    case keypadPress(String)
    case keypadClear
    case keypadBackspace
    case keypadOk
    case keypadSpace
    case setActiveField(ActiveField)
    case removeItem(String)
    case clearAllItems
    case checkout(PaymentMethodType)
    case setPaidAmount(String)
    case dismissWarning
    case dismissError
}
