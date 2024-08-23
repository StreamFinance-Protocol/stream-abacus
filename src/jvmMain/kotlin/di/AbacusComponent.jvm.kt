package exchange.stream.abacus.di

import exchange.stream.abacus.protocols.DataNotificationProtocol
import exchange.stream.abacus.protocols.PresentationProtocol
import exchange.stream.abacus.protocols.StateNotificationProtocol
import exchange.stream.abacus.state.v2.supervisor.AppConfigsV2
import exchange.stream.abacus.utils.IOImplementations
import exchange.stream.abacus.utils.UIImplementations

internal actual fun createAbacusComponent(
    deploymentUri: DeploymentUri,
    deployment: Deployment,
    appConfigs: AppConfigsV2,
    ioImplementations: IOImplementations,
    uiImplementations: UIImplementations,
    stateNotification: StateNotificationProtocol?,
    dataNotification: DataNotificationProtocol?,
    presentationProtocol: PresentationProtocol?,
): AbacusComponent = AbacusComponent::class.create(
    deploymentUri,
    deployment,
    appConfigs,
    ioImplementations,
    uiImplementations,
    stateNotification,
    dataNotification,
    presentationProtocol,
)
