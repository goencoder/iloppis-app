import Foundation

struct Event: Identifiable, Equatable {
    let id: String
    let name: String
    let description: String?
    let startTime: String?
    let endTime: String?
    let addressStreet: String?
    let addressCity: String?
    let lifecycleState: String?
}
