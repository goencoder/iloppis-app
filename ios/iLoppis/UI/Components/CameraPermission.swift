import AVFoundation
import Foundation

enum CameraAuthorizationState: Equatable {
    case authorized
    case denied
    case restricted
    case notDetermined
}

enum CameraPermission {
    static func currentState() -> CameraAuthorizationState {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: return .authorized
        case .denied: return .denied
        case .restricted: return .restricted
        case .notDetermined: return .notDetermined
        @unknown default: return .denied
        }
    }

    static func request() async -> CameraAuthorizationState {
        let granted = await AVCaptureDevice.requestAccess(for: .video)
        return granted ? .authorized : .denied
    }
}
